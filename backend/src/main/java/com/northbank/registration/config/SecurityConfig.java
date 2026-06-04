// Story: US-001 / US-002 / US-003 / US-005
package com.northbank.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * <p>Rules (ADR-001 / ADR-002 / ADR-005):</p>
 * <ul>
 *   <li>{@code POST /api/v1/customers}              — public (US-001)</li>
 *   <li>{@code POST /api/v1/auth/login}              — public (US-002)</li>
 *   <li>{@code POST /api/v1/auth/verify-otp}         — public (US-003)</li>
 *   <li>{@code POST /api/v1/auth/resend-otp}         — public (US-003)</li>
 *   <li>{@code POST /api/v1/auth/forgot-password}    — public (US-004)</li>
 *   <li>{@code POST /api/v1/auth/reset-password}     — public (US-004)</li>
 *   <li>{@code GET  /api/v1/profile}                 — authenticated, ACCESS JWT (US-005)</li>
 *   <li>{@code PATCH /api/v1/profile}                — authenticated, ACCESS JWT (US-005)</li>
 *   <li>{@code /api/v1/accounts/**}                  — authenticated, ACCESS JWT (US-006/007/008)</li>
 *   <li>{@code /api/v1/transactions/**}              — authenticated, ACCESS JWT (US-010/011/012)</li>
 *   <li>{@code /api/v1/admin/transactions/**}        — ADMIN role required (US-013)</li>
 *   <li>{@code /api/v1/fraud/**}                     — FRAUD_ANALYST role required (US-014+)</li>
 *   <li>{@code /api/v1/notifications/**}             — authenticated, ACCESS JWT (US-016)</li>
 *   <li>{@code /swagger-ui/**}, {@code /api-docs/**} — public (development)</li>
 *   <li>All other requests denied by default.</li>
 *   <li>CSRF disabled — stateless REST API.</li>
 *   <li>Sessions stateless — no HTTP session created.</li>
 * </ul>
 *
 * <p>US-005 adds {@link JwtAuthenticationFilter} to the chain, placed before
 * {@link UsernamePasswordAuthenticationFilter}. The filter enforces ACCESS token
 * authentication on all non-public paths and writes RFC 7807 401 responses
 * directly when tokens are absent, invalid, or invalidated by a password change.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity             http,
            JwtAuthenticationFilter  jwtAuthenticationFilter) throws Exception {
        http
            // CSRF not needed for a stateless REST API
            .csrf(AbstractHttpConfigurer::disable)

            // No HTTP sessions — pure token/stateless design
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Register JWT filter before the standard username/password filter (US-005)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth
                // ── US-001: Registration (anonymous) ─────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/customers").permitAll()

                // ── US-002: Login (anonymous) ─────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                // ── US-003: OTP verification and resend (anonymous) ───────
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/verify-otp").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/resend-otp").permitAll()

                // ── US-004: Password reset (anonymous) ────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/reset-password").permitAll()

                // ── US-005: Profile (authenticated — ACCESS JWT required) ──
                .requestMatchers(HttpMethod.GET,   "/api/v1/profile").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/profile").authenticated()

                // ── US-006 / US-007 / US-008: Accounts ────────────────────
                .requestMatchers("/api/v1/accounts/**").authenticated()

                // ── US-013: Admin transaction overview ────────────────────
                .requestMatchers("/api/v1/admin/transactions/**").hasRole("ADMIN")

                // ── EPIC-004: Fraud detection analyst tools ───────────────
                .requestMatchers("/api/v1/fraud/**").hasRole("FRAUD_ANALYST")

                // ── US-016: Customer notifications ────────────────────────
                .requestMatchers("/api/v1/notifications/**").authenticated()

                // ── US-010 / US-011 / US-012: Customer transactions ───────
                .requestMatchers("/api/v1/transactions/**").authenticated()

                // ── OpenAPI / Swagger UI (development convenience) ────────
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs",
                    "/api-docs/**"
                ).permitAll()

                // ── Deny everything else by default ───────────────────────
                .anyRequest().denyAll()
            );

        return http.build();
    }

    /**
     * {@link DaoAuthenticationProvider} wired with the customer-backed
     * {@link UserDetailsService} and the BCrypt {@link PasswordEncoder} (ADR-002).
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Exposes an {@link AuthenticationManager} bean backed by the
     * {@link DaoAuthenticationProvider}. Available for injection in future
     * filter or service classes (e.g. US-003 OTP verification).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider daoAuthenticationProvider) {
        return new ProviderManager(daoAuthenticationProvider);
    }
}
