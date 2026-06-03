// Story: US-003
package com.northbank.registration.auth;

import com.northbank.registration.auth.otp.OtpSession;
import com.northbank.registration.auth.otp.OtpSessionRepository;
import com.northbank.registration.auth.token.RefreshTokenRepository;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for US-003: Two-Factor Authentication via SMS.
 *
 * <p>Acceptance Criteria covered:</p>
 * <ul>
 *   <li>AC1 — Upon successful password verification a 6-digit OTP is generated and the
 *       OTP session is persisted in DB with correct fields (customerId, sessionTokenHash,
 *       invalidated=false, failedAttempts=0, expiresAt ~5 min in future).</li>
 *   <li>AC2 — The OTP expires after 5 minutes; verifying an expired OTP returns 401
 *       with message "Invalid or expired OTP".</li>
 *   <li>AC3 — On valid OTP submission via POST /api/v1/auth/verify-otp the system
 *       returns 200 with {@code {"accessToken": string, "refreshToken": string}}.
 *       The OTP session is consumed (invalidated=true). A refresh_token row is persisted.</li>
 *   <li>AC4 — On invalid OTP the API returns 401 with message "Invalid or expired OTP"
 *       plus {@code remainingAttempts} in RFC 7807 extensions. DB failedAttempts is
 *       incremented.</li>
 *   <li>AC5 — After 3 consecutive invalid OTP attempts the session is invalidated;
 *       subsequent attempts (including with the correct OTP) return 401.</li>
 * </ul>
 *
 * <p><strong>Transaction / data-isolation note:</strong> {@link IntegrationTestBase} is
 * {@code @Transactional}. All MockMvc calls run through {@code TestDispatcherServlet}
 * on the same test thread and therefore JOIN the outer test transaction. Service-layer
 * changes (including those accompanied by domain exceptions that carry
 * {@code noRollbackFor}) remain visible to subsequent repository reads within the same
 * test. The transaction is always rolled back after each test for full isolation.</p>
 */
@DisplayName("US-003 — Two-Factor Authentication via SMS Integration Tests")
class TwoFactorAuthIntegrationTest extends IntegrationTestBase {

    // ── Endpoint paths ────────────────────────────────────────────────────────

    private static final String LOGIN_ENDPOINT      = "/api/v1/auth/login";
    private static final String VERIFY_OTP_ENDPOINT = "/api/v1/auth/verify-otp";
    private static final String RESEND_OTP_ENDPOINT = "/api/v1/auth/resend-otp";

    // ── Test data constants ───────────────────────────────────────────────────

    /** Test email — unique per run; cleaned by @BeforeEach. */
    private static final String VALID_EMAIL    = "2fa.test@example.com";

    /** Password satisfying the application's complexity rule. */
    private static final String VALID_PASSWORD = "Password1!";

    // ── Test infrastructure ───────────────────────────────────────────────────

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OtpSessionRepository otpSessionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ── Setup ─────────────────────────────────────────────────────────────────

    /**
     * Cleans all three tables in dependency order before every test.
     *
     * <p>otp_sessions and refresh_tokens have FK → customers, so they must be
     * deleted first. Because {@link IntegrationTestBase} rolls back each test,
     * this is a safety net for data committed outside a transaction (e.g. via
     * {@code @Commit}-annotated tests in other classes).</p>
     */
    @BeforeEach
    void setUp() {
        otpSessionRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        customerRepository.deleteAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds and persists an {@code ACTIVE} customer with a real BCrypt hash
     * for {@link #VALID_PASSWORD}.
     *
     * <p>Runs in the current test transaction, so it is immediately visible to
     * any subsequent MockMvc call on the same thread.</p>
     */
    private Customer createActiveCustomer() {
        Customer customer = Customer.builder()
                .firstName("Two")
                .lastName("Factor")
                .email(VALID_EMAIL)
                .phoneNumber("+14155552671")
                .dateOfBirth(LocalDate.of(1990, 6, 15))
                .passwordHash(passwordEncoder.encode(VALID_PASSWORD))
                .status(CustomerStatus.ACTIVE)
                .build();
        return customerRepository.save(customer);
    }

    /**
     * Performs a login HTTP call with {@link #VALID_EMAIL} / {@link #VALID_PASSWORD}
     * and returns the raw {@code sessionToken} string from the response JSON.
     */
    private String loginAndGetSessionToken() throws Exception {
        String body = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, VALID_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("sessionToken").asText();
    }

    /**
     * Returns the single {@link OtpSession} present in the DB.
     *
     * <p>Assumes exactly one session exists (enforced by single-customer,
     * single-login test design).</p>
     */
    private OtpSession findOtpSession() {
        List<OtpSession> sessions = otpSessionRepository.findAll();
        assertThat(sessions)
                .as("Exactly one OTP session must exist at this point in the test")
                .hasSize(1);
        return sessions.get(0);
    }

    /**
     * Returns an OTP code that is guaranteed to differ from {@code correctOtp}.
     *
     * <p>Prevents accidental submission of the correct OTP in tests that
     * intentionally require a wrong answer.</p>
     */
    private String wrongOtp(String correctOtp) {
        return "999999".equals(correctOtp) ? "999998" : "999999";
    }

    /** Builds a valid JSON login request body. */
    private String loginBody(String email, String password) {
        return """
                { "email": "%s", "password": "%s" }
                """.formatted(email, password);
    }

    /** Builds a valid JSON verify-otp request body. */
    private String verifyOtpBody(String sessionToken, String otp) {
        return """
                { "sessionToken": "%s", "otp": "%s" }
                """.formatted(sessionToken, otp);
    }

    /** Builds a valid JSON resend-otp request body. */
    private String resendOtpBody(String sessionToken) {
        return """
                { "sessionToken": "%s" }
                """.formatted(sessionToken);
    }

    // =========================================================================
    // AC1 — OTP session created in DB after successful password verification
    // =========================================================================

    /**
     * AC1 (happy path) — After a successful login the {@code otp_sessions} table must
     * contain exactly one row with the correct {@code customer_id}, a 64-char
     * {@code session_token_hash}, {@code invalidated=false}, {@code failed_attempts=0},
     * and an expiry timestamp approximately 5 minutes in the future.
     */
    @Test
    @DisplayName("AC1 — Login creates OTP session row in DB with correct fields")
    void ac1_login_createsOtpSessionInDb() throws Exception {
        Customer customer = createActiveCustomer();
        loginAndGetSessionToken();

        OtpSession session = findOtpSession();

        assertThat(session.getCustomerId())
                .as("OTP session must reference the logged-in customer")
                .isEqualTo(customer.getId());
        assertThat(session.getSessionTokenHash())
                .as("sessionTokenHash must be a 64-character SHA-256 hex digest")
                .hasSize(64)
                .matches("[0-9a-f]{64}");
        assertThat(session.isInvalidated())
                .as("Session must not be invalidated on creation")
                .isFalse();
        assertThat(session.getFailedAttempts())
                .as("Failed attempts counter must start at 0")
                .isZero();
        assertThat(session.getExpiresAt())
                .as("OTP must expire approximately 5 minutes in the future (allow ±1 min skew)")
                .isAfter(OffsetDateTime.now().plusMinutes(4))
                .isBefore(OffsetDateTime.now().plusMinutes(6));
    }

    /**
     * AC1 (OTP format) — The persisted OTP code must be exactly 6 decimal digits.
     */
    @Test
    @DisplayName("AC1 — OTP code stored in DB is exactly 6 decimal digits")
    void ac1_otpCode_isSixDigits() throws Exception {
        createActiveCustomer();
        loginAndGetSessionToken();

        OtpSession session = findOtpSession();

        assertThat(session.getOtpCode())
                .as("OTP must match the 6-digit format \\d{6}")
                .matches("\\d{6}");
    }

    // =========================================================================
    // AC3 — Valid OTP submission returns 200 with access + refresh tokens
    // =========================================================================

    /**
     * AC3 (happy path) — Submitting the correct OTP returns HTTP 200 with a
     * non-blank {@code accessToken} and non-blank {@code refreshToken}.
     * A {@code refresh_tokens} row must be persisted, and the OTP session must
     * be marked {@code invalidated=true} (replay prevention).
     */
    @Test
    @DisplayName("AC3 — Valid OTP returns 200 with non-blank accessToken and refreshToken")
    void ac3_validOtp_returns200WithTokens() throws Exception {
        createActiveCustomer();
        String sessionToken = loginAndGetSessionToken();
        String correctOtp   = findOtpSession().getOtpCode();

        mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, correctOtp)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        // DB assertion 1: a refresh_token row must exist
        assertThat(refreshTokenRepository.count())
                .as("One refresh_token row must have been persisted after successful OTP")
                .isEqualTo(1);

        // DB assertion 2: OTP session must be consumed (invalidated) — prevents replay
        OtpSession sessionAfter = otpSessionRepository.findAll().get(0);
        assertThat(sessionAfter.isInvalidated())
                .as("OTP session must be invalidated after successful verification (replay prevention)")
                .isTrue();
    }

    /**
     * AC3 (JWT structure) — The returned {@code accessToken} must be a compact
     * JWT consisting of exactly 3 dot-separated Base64URL-encoded parts.
     */
    @Test
    @DisplayName("AC3 — accessToken returned after valid OTP is a valid JWT (header.payload.signature)")
    void ac3_accessToken_isValidJwt() throws Exception {
        createActiveCustomer();
        String sessionToken = loginAndGetSessionToken();
        String correctOtp   = findOtpSession().getOtpCode();

        String responseBody = mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, correctOtp)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(responseBody).get("accessToken").asText();
        String[] parts     = accessToken.split("\\.");

        assertThat(parts)
                .as("JWT must have exactly 3 dot-separated parts: header.payload.signature")
                .hasSize(3);
        assertThat(parts[0]).as("JWT header must be non-blank").isNotBlank();
        assertThat(parts[1]).as("JWT payload must be non-blank").isNotBlank();
        assertThat(parts[2]).as("JWT signature must be non-blank").isNotBlank();
    }

    // =========================================================================
    // AC4 — Invalid OTP returns 401 with remainingAttempts extension
    // =========================================================================

    /**
     * AC4 (first wrong attempt) — Submitting a wrong OTP returns HTTP 401 with the
     * RFC 7807 {@code detail} "Invalid or expired OTP" and {@code remainingAttempts=2}.
     * DB: {@code failed_attempts} is incremented to 1; session is NOT yet invalidated.
     */
    @Test
    @DisplayName("AC4 — Wrong OTP returns 401 with 'Invalid or expired OTP' and remainingAttempts=2")
    void ac4_invalidOtp_returns401WithRemainingAttempts() throws Exception {
        createActiveCustomer();
        String sessionToken = loginAndGetSessionToken();
        String correctOtp   = findOtpSession().getOtpCode();

        mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, wrongOtp(correctOtp))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Invalid or expired OTP"))
                .andExpect(jsonPath("$.remainingAttempts").value(2));

        // DB assertion: counter incremented, session still active
        OtpSession session = otpSessionRepository.findAll().get(0);
        assertThat(session.getFailedAttempts())
                .as("failedAttempts must be incremented to 1 after first wrong OTP")
                .isEqualTo(1);
        assertThat(session.isInvalidated())
                .as("Session must not be invalidated after only 1 wrong OTP (threshold is 3)")
                .isFalse();
    }

    /**
     * AC4 (counter decrements correctly) — After 2 wrong attempts {@code remainingAttempts}
     * must equal 1 and DB {@code failed_attempts} must be 2, session still active.
     */
    @Test
    @DisplayName("AC4 — remainingAttempts decreases correctly: after 2 wrong attempts it equals 1")
    void ac4_remainingAttempts_decreasesCorrectly() throws Exception {
        createActiveCustomer();
        String sessionToken = loginAndGetSessionToken();
        String correctOtp   = findOtpSession().getOtpCode();
        String bad          = wrongOtp(correctOtp);

        // First wrong attempt → remainingAttempts = 2
        mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, bad)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.remainingAttempts").value(2));

        // Second wrong attempt → remainingAttempts = 1
        mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, bad)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.remainingAttempts").value(1));

        // DB assertion: failedAttempts = 2, session still live
        OtpSession session = otpSessionRepository.findAll().get(0);
        assertThat(session.getFailedAttempts())
                .as("failedAttempts must equal 2 after two wrong OTP attempts")
                .isEqualTo(2);
        assertThat(session.isInvalidated())
                .as("Session must not be invalidated after only 2 wrong OTPs (threshold is 3)")
                .isFalse();
    }

    // =========================================================================
    // AC5 — 3 consecutive failures invalidate the session
    // =========================================================================

    /**
     * AC5 (session lock-out) — The 3rd consecutive wrong OTP attempt returns
     * {@code remainingAttempts=0}, and the DB row must show {@code invalidated=true}
     * with {@code failed_attempts=3}.
     */
    @Test
    @DisplayName("AC5 — Three consecutive wrong OTPs invalidate session; 3rd returns remainingAttempts=0")
    void ac5_threeFailedAttempts_invalidatesSession() throws Exception {
        createActiveCustomer();
        String sessionToken = loginAndGetSessionToken();
        String correctOtp   = findOtpSession().getOtpCode();
        String bad          = wrongOtp(correctOtp);

        // First wrong attempt
        mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, bad)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.remainingAttempts").value(2));

        // Second wrong attempt
        mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, bad)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.remainingAttempts").value(1));

        // Third wrong attempt — triggers lockout
        mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, bad)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.remainingAttempts").value(0));

        // DB assertion: session must now be invalidated
        OtpSession session = otpSessionRepository.findAll().get(0);
        assertThat(session.getFailedAttempts())
                .as("failedAttempts must equal 3 after three wrong OTP attempts")
                .isEqualTo(3);
        assertThat(session.isInvalidated())
                .as("Session must be invalidated after 3 consecutive wrong OTP attempts (AC5)")
                .isTrue();
    }

    /**
     * AC5 (invalidated session rejects correct OTP) — After the session is invalidated
     * by 3 consecutive failures, submitting the correct OTP must still return 401.
     * The session must not be resurrected by a correct guess.
     */
    @Test
    @DisplayName("AC5 — Correct OTP submitted against an invalidated session still returns 401")
    void ac5_invalidatedSession_returns401OnCorrectOtp() throws Exception {
        createActiveCustomer();
        String sessionToken = loginAndGetSessionToken();
        OtpSession session  = findOtpSession();
        String correctOtp   = session.getOtpCode();
        String bad          = wrongOtp(correctOtp);

        // Burn 3 wrong attempts to force invalidation
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(verifyOtpBody(sessionToken, bad)));
        }

        // Verify session is indeed invalidated before the critical assertion
        OtpSession invalidated = otpSessionRepository.findAll().get(0);
        assertThat(invalidated.isInvalidated())
                .as("Pre-condition: session must be invalidated before submitting correct OTP")
                .isTrue();

        // Correct OTP on an invalidated session must still return 401
        mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, correctOtp)))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // AC2 — Expired OTP returns 401
    // =========================================================================

    /**
     * AC2 — After manipulating the OTP session's {@code expires_at} to a past
     * timestamp (simulating expiry), submitting the correct OTP must return 401
     * with the RFC 7807 {@code detail} "Invalid or expired OTP".
     */
    @Test
    @DisplayName("AC2 — Expired OTP (expires_at in the past) returns 401 with 'Invalid or expired OTP'")
    void ac2_expiredOtp_returns401() throws Exception {
        createActiveCustomer();
        String sessionToken = loginAndGetSessionToken();

        // Force the session to look expired by back-dating its expiry timestamp
        OtpSession session = findOtpSession();
        String correctOtp  = session.getOtpCode();
        session.setExpiresAt(OffsetDateTime.now().minusMinutes(1));
        otpSessionRepository.save(session);

        mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, correctOtp)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Invalid or expired OTP"));
    }

    // =========================================================================
    // Resend OTP — regeneration and rate-limiting
    // =========================================================================

    /**
     * Resend OTP (regeneration) — When enough time has elapsed since the last OTP
     * was issued (simulated by back-dating {@code created_at} by 61 seconds), the
     * resend endpoint returns 200 with a confirmation message.
     *
     * <p>DB assertions: the {@code otp_code} is still a valid 6-digit string,
     * {@code failed_attempts} is reset to 0, {@code expires_at} is refreshed to
     * approximately 5 minutes in the future, and the session is not invalidated.</p>
     */
    @Test
    @DisplayName("Resend OTP — returns 200 and refreshes the OTP session after the cool-down window")
    void resendOtp_regeneratesOtpCode() throws Exception {
        createActiveCustomer();
        String sessionToken = loginAndGetSessionToken();

        // Back-date the session's createdAt so the 60-second rate-limit window has passed
        OtpSession session   = findOtpSession();
        String originalOtp   = session.getOtpCode();
        session.setCreatedAt(OffsetDateTime.now().minusSeconds(61));
        otpSessionRepository.save(session);

        // Resend must succeed
        mockMvc.perform(post(RESEND_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resendOtpBody(sessionToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        // DB assertions after resend
        OtpSession updated = otpSessionRepository.findAll().get(0);

        assertThat(updated.getOtpCode())
                .as("Resent OTP must still be a valid 6-digit code")
                .matches("\\d{6}");
        assertThat(updated.getFailedAttempts())
                .as("failedAttempts must be reset to 0 on resend")
                .isZero();
        assertThat(updated.isInvalidated())
                .as("Session must remain active after resend")
                .isFalse();
        assertThat(updated.getExpiresAt())
                .as("expiresAt must be refreshed to ~5 minutes in the future")
                .isAfter(OffsetDateTime.now().plusMinutes(4))
                .isBefore(OffsetDateTime.now().plusMinutes(6));
        // createdAt reset: proves the rate-limit window was restarted
        assertThat(updated.getCreatedAt())
                .as("createdAt must have been reset to approximately now by resend")
                .isAfter(OffsetDateTime.now().minusSeconds(10));

        // Bonus: assert the OTP code actually changed (1/1,000,000 false-failure risk — acceptable)
        assertThat(updated.getOtpCode())
                .as("Resent OTP code should differ from the original code")
                .isNotEqualTo(originalOtp);
    }

    /**
     * Resend OTP (rate-limit) — Calling resend immediately after login (i.e. within
     * the 60-second cool-down window) must return 429 Too Many Requests with a
     * {@code Retry-After: 60} response header.
     *
     * <p>After the first resend succeeds (by back-dating {@code created_at}), a
     * second immediate resend must fail with 429 because the resend itself resets
     * {@code created_at} to now.</p>
     */
    @Test
    @DisplayName("Resend OTP — 429 with Retry-After:60 when called within 60 seconds of last OTP issuance")
    void resendOtp_rateLimitWithin60Seconds_returns429() throws Exception {
        createActiveCustomer();
        String sessionToken = loginAndGetSessionToken();

        // Back-date so first resend passes the rate-limit check (createdAt = 61s ago)
        OtpSession session = findOtpSession();
        session.setCreatedAt(OffsetDateTime.now().minusSeconds(61));
        otpSessionRepository.save(session);

        // First resend: should succeed (rate limit window has passed)
        mockMvc.perform(post(RESEND_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resendOtpBody(sessionToken)))
                .andExpect(status().isOk());

        // Second resend immediately: createdAt was just reset → within 60-second window → 429
        mockMvc.perform(post(RESEND_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resendOtpBody(sessionToken)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}
