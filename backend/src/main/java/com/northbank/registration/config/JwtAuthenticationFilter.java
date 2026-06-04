// Story: US-005
package com.northbank.registration.config;

import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.repository.CustomerRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JWT authentication filter for secured endpoints (US-005, ADR-005).
 *
 * <p>This filter enforces AC6 (unauthenticated requests → 401) and the
 * ADR-004 session-invalidation rule (JWT {@code iat} &lt; {@code password_changed_at} → 401).</p>
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Public paths (registration, login, OTP, password-reset, Swagger) are
 *       bypassed via {@link #shouldNotFilter(HttpServletRequest)}.</li>
 *   <li>Extract {@code Authorization: Bearer &lt;token&gt;} header. Missing or
 *       malformed → write RFC 7807 401 and stop.</li>
 *   <li>Call {@link JwtConfig#validateAccessToken(String)} — verifies signature,
 *       expiry and {@code type == "ACCESS"}. Failure → 401.</li>
 *   <li>Load {@link Customer} by UUID ({@code sub} claim). Not found → 401.</li>
 *   <li>Enforce password-change invalidation: if {@code iat &lt; password_changed_at}
 *       → 401 with detail "Session invalidated. Please sign in again."</li>
 *   <li>Populate {@code SecurityContextHolder} with
 *       {@code UsernamePasswordAuthenticationToken(UUID, null, [])}.
 *       Downstream controllers receive the UUID via {@code @AuthenticationPrincipal}.</li>
 *   <li>Invoke {@code filterChain.doFilter}.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig            jwtConfig;
    private final CustomerRepository   customerRepository;

    /**
     * Exact public paths that bypass JWT validation.
     * Prefix-matched paths (Swagger) are handled separately in {@link #shouldNotFilter}.
     */
    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/api/v1/customers",
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/resend-otp"
    );

    // ── shouldNotFilter ──────────────────────────────────────────────────────

    /**
     * Skips this filter for public endpoints so that anonymous traffic is never
     * blocked here.  Spring Security's authorization rules then independently
     * control access via {@code permitAll()} on those paths.
     *
     * <p><strong>Path resolution:</strong> {@code getServletPath()} returns {@code ""}
     * (empty string) on {@code MockHttpServletRequest} objects created by Spring MockMvc,
     * because the servlet path is only populated by the real {@code DispatcherServlet}
     * during request processing — after this method is called.  Using
     * {@code getRequestURI()} (minus the context path) gives the correct path in both
     * the embedded-server / production and the MockMvc / test environments.</p>
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri         = request.getRequestURI();
        String contextPath = request.getContextPath();
        // Strip context path (e.g. "/app") if the application is deployed under one
        String path = (!contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length())
                : uri;
        return PUBLIC_EXACT_PATHS.contains(path)
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs");
    }

    // ── doFilterInternal ────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain) throws ServletException, IOException {

        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Step 1: Extract Bearer token ─────────────────────────────────────
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, request.getRequestURI(),
                    "Authentication required. Please provide a valid access token.");
            return;
        }
        String rawToken = authHeader.substring(7);

        // ── Step 2: Validate ACCESS token ─────────────────────────────────────
        Claims claims;
        try {
            claims = jwtConfig.validateAccessToken(rawToken);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("ACCESS token validation failed for URI {}: {}", request.getRequestURI(), ex.getMessage());
            writeUnauthorized(response, request.getRequestURI(),
                    "Authentication required. Please provide a valid access token.");
            return;
        }

        // ── Step 3: Resolve customer UUID from sub claim ──────────────────────
        UUID customerId;
        try {
            customerId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException ex) {
            log.warn("ACCESS token sub is not a valid UUID: {}", claims.getSubject());
            writeUnauthorized(response, request.getRequestURI(),
                    "Authentication required. Please provide a valid access token.");
            return;
        }

        // ── Step 4: Load customer — confirms identity still exists in DB ──────
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            log.warn("Customer not found for JWT sub={} at URI={}", customerId, request.getRequestURI());
            writeUnauthorized(response, request.getRequestURI(),
                    "Authentication required. Please provide a valid access token.");
            return;
        }

        // ── Step 5: Enforce password_changed_at invalidation (ADR-004 AC5) ────
        if (customer.getPasswordChangedAt() != null) {
            long iatSeconds = claims.getIssuedAt().toInstant().getEpochSecond();
            Instant issuedAt = Instant.ofEpochSecond(iatSeconds);
            if (issuedAt.isBefore(customer.getPasswordChangedAt().toInstant())) {
                log.warn("TOKEN INVALIDATED — iat={} is before passwordChangedAt={} for customerId={}",
                        issuedAt, customer.getPasswordChangedAt(), customerId);
                writeUnauthorized(response, request.getRequestURI(),
                        "Session invalidated. Please sign in again.");
                return;
            }
        }

        // ── Step 6: Set SecurityContext — principal = customer UUID ───────────
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(customerId, null, resolveAuthorities(claims));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // ── Step 7: Continue filter chain ────────────────────────────────────
        filterChain.doFilter(request, response);
    }

    // ── Helper: write RFC 7807 401 directly to response ─────────────────────

    private void writeUnauthorized(HttpServletResponse response, String instance, String detail)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        // Escape forward-slashes in instance to avoid JSON issues with paths
        String escapedInstance = instance.replace("\"", "\\\"");
        String escapedDetail   = detail.replace("\"", "\\\"");
        String body = String.format(
                "{\"type\":\"https://api.bank.example/problems/unauthorized\"," +
                "\"title\":\"Unauthorized\"," +
                "\"status\":401," +
                "\"detail\":\"%s\"," +
                "\"instance\":\"%s\"}",
                escapedDetail, escapedInstance
        );
        response.getWriter().write(body);
    }

    private List<SimpleGrantedAuthority> resolveAuthorities(Claims claims) {
        Object rolesClaim = claims.get("roles");
        if (rolesClaim == null) {
            rolesClaim = claims.get("authorities");
        }
        if (!(rolesClaim instanceof List<?> values)) {
            return Collections.emptyList();
        }

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        for (Object value : values) {
            if (value instanceof String role && !role.isBlank()) {
                String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                authorities.add(new SimpleGrantedAuthority(authority));
            }
        }
        return authorities;
    }
}
