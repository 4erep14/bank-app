// Story: US-004
package com.northbank.registration.auth;

import com.northbank.registration.auth.passwordreset.PasswordResetToken;
import com.northbank.registration.auth.passwordreset.PasswordResetTokenRepository;
import com.northbank.registration.config.IntegrationTestBase;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for US-004: Password Reset
 *
 * Acceptance Criteria covered:
 * - AC1: POST /api/v1/auth/forgot-password always returns 200 (anti-enumeration)
 * - AC2: Known email → token row created in DB with used=false
 * - AC3: Weak new password → 400 validation error
 * - AC4: Valid token + strong password → 200, password updated in DB
 * - AC5: After reset, password_changed_at is set in DB
 * - AC6: Expired token → 400 "Invalid or expired reset token"
 * - AC7: Already-used token → 400 "Invalid or expired reset token"
 */
class PasswordResetIntegrationTest extends IntegrationTestBase {

    @Autowired private CustomerRepository customerRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String EMAIL = "reset.test@northbank.com";
    private static final String RAW_PASSWORD = "0ldP@ssw0rd!";

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        customerRepository.deleteAll();
    }

    private Customer activeCustomer() {
        return customerRepository.save(Customer.builder()
                .firstName("Reset").lastName("Tester").email(EMAIL)
                .phoneNumber("+44987654321").dateOfBirth(LocalDate.of(1985, 6, 15))
                .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                .status(CustomerStatus.ACTIVE).build());
    }

    private String sha256Hex(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // ── AC1: Always 200 ───────────────────────────────────────────────────────

    @Test
    @DisplayName("AC1 — Unknown email still returns 200 (anti-enumeration)")
    void ac1_unknownEmail_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@northbank.com\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("AC1 — Known email also returns 200 (same response)")
    void ac1_knownEmail_returns200() throws Exception {
        activeCustomer();
        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(EMAIL)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    // ── AC2: Token created in DB ──────────────────────────────────────────────

    @Test
    @DisplayName("AC2 — Known email causes a token row to be inserted with used=false")
    void ac2_knownEmail_createsTokenInDb() throws Exception {
        Customer c = activeCustomer();
        mockMvc.perform(post("/api/v1/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(EMAIL)))
            .andExpect(status().isOk());

        assertThat(tokenRepository.findAll())
            .hasSize(1)
            .first()
            .satisfies(t -> {
                assertThat(t.getCustomer().getId()).isEqualTo(c.getId());
                assertThat(t.isUsed()).isFalse();
                assertThat(t.getExpiresAt()).isAfter(OffsetDateTime.now());
            });
    }

    // ── AC3: Weak password rejected ───────────────────────────────────────────

    @Test
    @DisplayName("AC3 — Weak new password in reset returns 400 validation error")
    void ac3_weakPassword_returns400() throws Exception {
        Customer c = activeCustomer();
        String rawToken = "someRawToken12345678901234567890123";
        String hash = sha256Hex(rawToken);
        tokenRepository.save(PasswordResetToken.builder()
                .customer(c).tokenHash(hash)
                .expiresAt(OffsetDateTime.now().plusHours(1)).build());

        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"newPassword\":\"weak\"}".formatted(rawToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isArray());
    }

    // ── AC4/AC5: Valid reset ───────────────────────────────────────────────────

    @Test
    @DisplayName("AC4/AC5 — Valid token + strong password → 200, DB updated, passwordChangedAt set")
    void ac4_validReset_updatesPasswordAndSetsChangedAt() throws Exception {
        Customer c = activeCustomer();
        String rawToken = "validToken1234567890123456789012345";
        String hash = sha256Hex(rawToken);
        tokenRepository.save(PasswordResetToken.builder()
                .customer(c).tokenHash(hash)
                .expiresAt(OffsetDateTime.now().plusHours(1)).build());

        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"newPassword\":\"N3wStr0ng!Pass\"}".formatted(rawToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").isNotEmpty());

        Customer updated = customerRepository.findById(c.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("N3wStr0ng!Pass", updated.getPasswordHash())).isTrue();
        assertThat(updated.getPasswordChangedAt()).isNotNull();  // AC5
    }

    // ── AC6: Expired token ────────────────────────────────────────────────────

    @Test
    @DisplayName("AC6 — Expired token → 400 'Invalid or expired reset token'")
    void ac6_expiredToken_returns400() throws Exception {
        Customer c = activeCustomer();
        String rawToken = "expiredToken123456789012345678901234";
        String hash = sha256Hex(rawToken);
        tokenRepository.save(PasswordResetToken.builder()
                .customer(c).tokenHash(hash)
                .expiresAt(OffsetDateTime.now().minusMinutes(1)).build());  // already expired

        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"newPassword\":\"N3wStr0ng!Pass\"}".formatted(rawToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid or expired reset token"));
    }

    // ── AC7: Used token ───────────────────────────────────────────────────────

    @Test
    @DisplayName("AC7 — Already-used token → 400")
    void ac7_usedToken_returns400() throws Exception {
        Customer c = activeCustomer();
        String rawToken = "usedToken12345678901234567890123456";
        String hash = sha256Hex(rawToken);
        tokenRepository.save(PasswordResetToken.builder()
                .customer(c).tokenHash(hash)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .used(true).build());  // already used

        mockMvc.perform(post("/api/v1/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"newPassword\":\"N3wStr0ng!Pass\"}".formatted(rawToken)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("Invalid or expired reset token"));
    }
}
