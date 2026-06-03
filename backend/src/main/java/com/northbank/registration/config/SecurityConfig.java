// Story: US-001 / US-002 / US-003 / US-005 / US-009
package com.northbank.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
 * <p>US-009 changes:</p>
 * <ul>
 *   <li>{@code /api/v1/admin/**} requires {@code ROLE_ADMIN} (AC6).</li>
 *   <li>All {@code /api/v1/accounts/**} endpoints require authentication.</li>
 *   <li>{@code @EnableMethodSecurity} enabled for future {@code @PreAuthorize} use.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity             http,
            JwtAuthenticationFilter  jwtAuthenticationFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth
                // ── Public: Registration ──────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/customers").permitAll()

                // ── Public: Auth flows ────────────────────────────────────
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/verify-otp").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/resend-otp").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/reset-password").permitAll()

                // ── Authenticated: Profile ────────────────────────────────
                .requestMatchers(HttpMethod.GET,   "/api/v1/profile").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/profile").authenticated()

                // ── Authenticated: Customer account endpoints ─────────────
                .requestMatchers("/api/v1/accounts").authenticated()
                .requestMatchers("/api/v1/accounts/**").authenticated()

                // ── ROLE_ADMIN only: Admin endpoints (US-009, AC6) ────────
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // ── OpenAPI / Swagger UI ──────────────────────────────────
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/api-docs",
                    "/api-docs/**"
                ).permitAll()

                // ── Deny everything else ──────────────────────────────────
                .anyRequest().denyAll()
            );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            DaoAuthenticationProvider daoAuthenticationProvider) {
        return new ProviderManager(daoAuthenticationProvider);
    }
}
