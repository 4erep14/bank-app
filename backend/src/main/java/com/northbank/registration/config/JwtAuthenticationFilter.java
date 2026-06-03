// Story: US-005 / US-009
package com.northbank.registration.config;

import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerRole;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * JWT authentication filter for secured endpoints (US-005, US-009, ADR-005).
 *
 * <p>US-009 update: the {@code role} claim from the ACCESS token is now extracted
 * and converted to a Spring Security {@link GrantedAuthority} so that
 * {@code @PreAuthorize("hasRole('ADMIN')")} and
 * {@code .hasRole("ADMIN")} in {@link SecurityConfig} work correctly.</p>
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Bypass public paths via {@link #shouldNotFilter(HttpServletRequest)}.</li>
 *   <li>Extract {@code Authorization: Bearer} header. Missing → 401.</li>
 *   <li>Validate ACCESS token signature + expiry + type. Failure → 401.</li>
 *   <li>Resolve {@link Customer} by UUID sub claim. Not found → 401.</li>
 *   <li>Enforce {@code iat < password_changed_at} invalidation → 401.</li>
 *   <li>Build {@link GrantedAuthority} list from {@code role} claim (e.g. ROLE_ADMIN).</li>
 *   <li>Populate SecurityContext with {@code (customerId, authorities)}.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtConfig          jwtConfig;
    private final CustomerRepository customerRepository;

    private static final Set<String> PUBLIC_EXACT_PATHS = Set.of(
            "/api/v1/customers",
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/resend-otp"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri         = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = (!contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length())
                : uri;
        return PUBLIC_EXACT_PATHS.contains(path)
                || path.startsWith("/swagger-ui")
                || path.startsWith("/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain) throws ServletException, IOException {

        // Step 1: Extract Bearer token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorized(response, request.getRequestURI(),
                    "Authentication required. Please provide a valid access token.");
            return;
        }
        String rawToken = authHeader.substring(7);

        // Step 2: Validate ACCESS token
        Claims claims;
        try {
            claims = jwtConfig.validateAccessToken(rawToken);
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("ACCESS token validation failed for URI {}: {}", request.getRequestURI(), ex.getMessage());
            writeUnauthorized(response, request.getRequestURI(),
                    "Authentication required. Please provide a valid access token.");
            return;
        }

        // Step 3: Resolve customer UUID from sub claim
        UUID customerId;
        try {
            customerId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException ex) {
            log.warn("ACCESS token sub is not a valid UUID: {}", claims.getSubject());
            writeUnauthorized(response, request.getRequestURI(),
                    "Authentication required. Please provide a valid access token.");
            return;
        }

        // Step 4: Load customer — confirms identity still exists in DB
        Customer customer = customerRepository.findById(customerId).orElse(null);
        if (customer == null) {
            log.warn("Customer not found for JWT sub={} at URI={}", customerId, request.getRequestURI());
            writeUnauthorized(response, request.getRequestURI(),
                    "Authentication required. Please provide a valid access token.");
            return;
        }

        // Step 5: Enforce password_changed_at invalidation (ADR-004)
        if (customer.getPasswordChangedAt() != null) {
            long iatSeconds = claims.getIssuedAt().toInstant().getEpochSecond();
            Instant issuedAt = Instant.ofEpochSecond(iatSeconds);
            if (issuedAt.isBefore(customer.getPasswordChangedAt().toInstant())) {
                log.warn("TOKEN INVALIDATED — iat={} before passwordChangedAt={} for customerId={}",
                        issuedAt, customer.getPasswordChangedAt(), customerId);
                writeUnauthorized(response, request.getRequestURI(),
                        "Session invalidated. Please sign in again.");
                return;
            }
        }

        // Step 6: Build authorities from role claim (US-009)
        // The role claim is stored as the enum name, e.g. "ADMIN" or "CUSTOMER".
        // Spring Security's hasRole('ADMIN') matches against "ROLE_ADMIN", so we
        // prefix with "ROLE_" when constructing the SimpleGrantedAuthority.
        String roleClaim = claims.get("role", String.class);
        CustomerRole resolvedRole;
        try {
            resolvedRole = (roleClaim != null)
                    ? CustomerRole.valueOf(roleClaim)
                    : CustomerRole.CUSTOMER;
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown role claim '{}' in JWT for customerId={}", roleClaim, customerId);
            resolvedRole = CustomerRole.CUSTOMER;
        }
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + resolvedRole.name())
        );

        // Step 7: Populate SecurityContext with customerId + authorities
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(customerId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String instance, String detail)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
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
}
