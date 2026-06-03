// Story: US-005
package com.northbank.registration.profile;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for US-005: View &amp; Update Customer Profile.
 *
 * <p>Acceptance Criteria covered:</p>
 * <ul>
 *   <li>AC1 — Authenticated customer can call {@code GET /api/v1/profile} and receive:
 *       firstName, lastName, email, phoneNumber, dateOfBirth.</li>
 *   <li>AC2 — Customer can call {@code PATCH /api/v1/profile} to update firstName,
 *       lastName, and phoneNumber; updated values are returned and persisted.</li>
 *   <li>AC3 — email and dateOfBirth are read-only; any attempt to update them returns
 *       400 with detail "Field is not editable".</li>
 *   <li>AC4 — Updated phoneNumber must pass E.164 format validation; invalid format
 *       returns 400 with field-level error.</li>
 *   <li>AC5 — Profile changes are persisted and returned correctly on the next
 *       {@code GET /api/v1/profile} call; DB state is asserted.</li>
 *   <li>AC6 — Unauthenticated requests (missing auth, invalid token, SESSION token
 *       used instead of ACCESS token) to either endpoint return 401.</li>
 * </ul>
 *
 * <p><strong>Transaction / data-isolation note:</strong> {@link IntegrationTestBase} is
 * {@code @Transactional}. All MockMvc calls (via {@code TestDispatcherServlet}) run
 * synchronously on the same test thread and therefore JOIN the outer test transaction.
 * Service-layer writes are immediately visible to subsequent repository reads within
 * the same test. The transaction is always rolled back after each test for full
 * isolation. The {@code @BeforeEach} cleanup is an additional safety net.</p>
 *
 * <p><strong>Access-token acquisition:</strong> every test that needs an authenticated
 * call obtains its ACCESS JWT via the private {@link #getAccessToken(String, String)}
 * helper, which executes the full HTTP flow:
 * register → login → read OTP from DB → verify-otp → return accessToken.</p>
 */
@DisplayName("US-005 — View & Update Customer Profile Integration Tests")
class CustomerProfileIntegrationTest extends IntegrationTestBase {

    // ── Endpoint constants ────────────────────────────────────────────────────

    private static final String PROFILE_ENDPOINT     = "/api/v1/profile";
    private static final String REGISTER_ENDPOINT    = "/api/v1/customers";
    private static final String LOGIN_ENDPOINT       = "/api/v1/auth/login";
    private static final String VERIFY_OTP_ENDPOINT  = "/api/v1/auth/verify-otp";

    // ── Test-data constants ───────────────────────────────────────────────────

    /** Canonical test email; unique within each test (data is rolled back between tests). */
    private static final String VALID_EMAIL    = "profile.test@example.com";

    /** Password satisfying the application's complexity rule (≥8, upper, lower, digit, special). */
    private static final String VALID_PASSWORD = "Password1!";

    /** Valid E.164 phone number used on registration. */
    private static final String VALID_PHONE    = "+14155551234";

    // ── Test infrastructure ───────────────────────────────────────────────────

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OtpSessionRepository otpSessionRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    /**
     * Real BCrypt encoder (strength 12) — used to produce a valid hash for
     * customers created directly via the repository in AC6 session-token tests.
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    // ── Setup ─────────────────────────────────────────────────────────────────

    /**
     * Wipes all three tables in FK-dependency order before every test.
     *
     * <p>{@code otp_sessions} and {@code refresh_tokens} have a FK → {@code customers},
     * so they must be deleted first. This is a safety net; {@link IntegrationTestBase}'s
     * {@code @Transactional} rolls back each test's data automatically.</p>
     */
    @BeforeEach
    void setUp() {
        otpSessionRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        customerRepository.deleteAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Obtains a valid ACCESS JWT via the full HTTP flow (US-005 specification):
     * <ol>
     *   <li>Register a new customer via {@code POST /api/v1/customers}.</li>
     *   <li>Log in via {@code POST /api/v1/auth/login} to obtain a SESSION JWT.</li>
     *   <li>Read the OTP code from the single {@link OtpSession} row created in DB.</li>
     *   <li>Verify the OTP via {@code POST /api/v1/auth/verify-otp} to obtain the ACCESS JWT.</li>
     * </ol>
     *
     * <p>All four HTTP calls join the enclosing test transaction (same thread,
     * {@code TestDispatcherServlet}), so the registered customer and OTP session are
     * visible to subsequent repository reads without committing.</p>
     *
     * @param email    customer email address (must be unique within the test)
     * @param password raw password satisfying the application's complexity rules
     * @return compact ACCESS JWT string ready for {@code Authorization: Bearer <token>}
     */
    private String getAccessToken(String email, String password) throws Exception {

        // Step 1 — Register customer (PENDING_VERIFICATION status; login only blocks LOCKED)
        String registerBody = """
                {
                  "firstName":   "Profile",
                  "lastName":    "Tester",
                  "email":       "%s",
                  "phoneNumber": "%s",
                  "dateOfBirth": "1990-06-15",
                  "password":    "%s"
                }
                """.formatted(email, VALID_PHONE, password);

        mockMvc.perform(post(REGISTER_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        // Step 2 — Login → SESSION token
        String loginResponse = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String sessionToken = objectMapper.readTree(loginResponse).get("sessionToken").asText();

        // Step 3 — Read OTP from DB (exactly one session exists at this point in the test)
        List<OtpSession> sessions = otpSessionRepository.findAll();
        assertThat(sessions)
                .as("Exactly one OTP session must exist after login")
                .hasSize(1);
        String otp = sessions.get(0).getOtpCode();

        // Step 4 — Verify OTP → ACCESS token
        String verifyResponse = mockMvc.perform(post(VERIFY_OTP_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyOtpBody(sessionToken, otp)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(verifyResponse).get("accessToken").asText();
    }

    /**
     * Creates and persists an {@code ACTIVE} customer directly via the repository.
     *
     * <p>Used in AC6 session-token tests where only a login step is needed (not the
     * full registration → OTP flow). The ACTIVE status is not strictly required for
     * login (AuthService only blocks LOCKED accounts), but it matches the canonical
     * "fully onboarded" customer state used in other test classes.</p>
     */
    private Customer createActiveCustomer(String email) {
        Customer customer = Customer.builder()
                .firstName("Profile")
                .lastName("Tester")
                .email(email)
                .phoneNumber(VALID_PHONE)
                .dateOfBirth(LocalDate.of(1990, 6, 15))
                .passwordHash(passwordEncoder.encode(VALID_PASSWORD))
                .status(CustomerStatus.ACTIVE)
                .build();
        return customerRepository.save(customer);
    }

    /** Builds a JSON login request body. */
    private String loginBody(String email, String password) {
        return """
                { "email": "%s", "password": "%s" }
                """.formatted(email, password);
    }

    /** Builds a JSON verify-otp request body. */
    private String verifyOtpBody(String sessionToken, String otp) {
        return """
                { "sessionToken": "%s", "otp": "%s" }
                """.formatted(sessionToken, otp);
    }

    // =========================================================================
    // AC1 — GET /api/v1/profile returns all five profile fields
    // =========================================================================

    /**
     * AC1 (happy path) — An authenticated customer calling {@code GET /api/v1/profile}
     * receives HTTP 200 with a JSON body containing all five required fields:
     * {@code firstName}, {@code lastName}, {@code email}, {@code phoneNumber},
     * and {@code dateOfBirth}.
     *
     * <p>The email and phone number values must match the registration data
     * (field values asserted, not just presence). The ACCESS JWT is obtained via
     * the full registration → OTP flow.</p>
     */
    @Test
    @DisplayName("AC1 — GET /api/v1/profile returns 200 with all 5 fields: firstName, lastName, email, phoneNumber, dateOfBirth")
    void ac1_getProfile_returnsAllFields() throws Exception {
        String accessToken = getAccessToken(VALID_EMAIL, VALID_PASSWORD);

        mockMvc.perform(get(PROFILE_ENDPOINT)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // All five AC1 fields must be present and non-empty
                .andExpect(jsonPath("$.firstName").value("Profile"))
                .andExpect(jsonPath("$.lastName").value("Tester"))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-06-15"))
                // Safety: hash/password must never leak into a response
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    // =========================================================================
    // AC2 — PATCH /api/v1/profile updates firstName, lastName, phoneNumber
    // =========================================================================

    /**
     * AC2 (happy path) — A {@code PATCH /api/v1/profile} request carrying all three
     * mutable fields ({@code firstName}, {@code lastName}, {@code phoneNumber}) must
     * return HTTP 200 with the updated values reflected in the response body, and
     * the changes must be persisted in the database.
     */
    @Test
    @DisplayName("AC2 — PATCH /api/v1/profile with valid firstName/lastName/phoneNumber returns 200 with updated values")
    void ac2_patchProfile_updatesEditableFields() throws Exception {
        String accessToken = getAccessToken(VALID_EMAIL, VALID_PASSWORD);

        String patchBody = """
                {
                  "firstName":   "Updated",
                  "lastName":    "Name",
                  "phoneNumber": "+12125551234"
                }
                """;

        mockMvc.perform(patch(PROFILE_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"))
                .andExpect(jsonPath("$.phoneNumber").value("+12125551234"))
                // Read-only fields must be preserved unchanged
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-06-15"));

        // DB assertion — persisted changes must match
        Customer saved = customerRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Updated");
        assertThat(saved.getLastName()).isEqualTo("Name");
        assertThat(saved.getPhoneNumber()).isEqualTo("+12125551234");
    }

    // =========================================================================
    // AC3 — email is read-only; attempting to update it returns 400
    // =========================================================================

    /**
     * AC3 — Sending {@code "email"} in a PATCH request must return HTTP 400 with
     * RFC 7807 {@code detail} "Field is not editable". The email stored in the DB
     * must remain unchanged.
     *
     * <p>Enforcement mechanism: {@code @JsonAnySetter} on {@link UpdateProfileRequest}
     * throws {@link com.northbank.registration.profile.exception.FieldNotEditableException},
     * which the {@code GlobalExceptionHandler} maps to 400.</p>
     */
    @Test
    @DisplayName("AC3 — PATCH /api/v1/profile with 'email' field returns 400 'Field is not editable'")
    void ac3_patchProfile_emailReadOnly_returns400() throws Exception {
        String accessToken = getAccessToken(VALID_EMAIL, VALID_PASSWORD);

        String patchBody = """
                { "email": "newemail@test.com" }
                """;

        mockMvc.perform(patch(PROFILE_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(patchBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Field is not editable"));

        // DB assertion — email must NOT have been changed
        Customer saved = customerRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertThat(saved.getEmail())
                .as("Email must remain unchanged after a rejected PATCH attempt")
                .isEqualTo(VALID_EMAIL);
    }

    // =========================================================================
    // AC3 — dateOfBirth is read-only; attempting to update it returns 400
    // =========================================================================

    /**
     * AC3 — Sending {@code "dateOfBirth"} in a PATCH request must return HTTP 400
     * with RFC 7807 {@code detail} containing "not editable". The dateOfBirth
     * stored in the DB must remain unchanged.
     */
    @Test
    @DisplayName("AC3 — PATCH /api/v1/profile with 'dateOfBirth' field returns 400 'Field is not editable'")
    void ac3_patchProfile_dateOfBirthReadOnly_returns400() throws Exception {
        String accessToken = getAccessToken(VALID_EMAIL, VALID_PASSWORD);

        String patchBody = """
                { "dateOfBirth": "1990-01-01" }
                """;

        mockMvc.perform(patch(PROFILE_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(patchBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("not editable")));

        // DB assertion — dateOfBirth must NOT have been changed
        Customer saved = customerRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertThat(saved.getDateOfBirth())
                .as("dateOfBirth must remain unchanged after a rejected PATCH attempt")
                .isEqualTo(LocalDate.of(1990, 6, 15));
    }

    // =========================================================================
    // AC4 — Invalid E.164 phoneNumber returns 400 with field-level error
    // =========================================================================

    /**
     * AC4 — A {@code phoneNumber} value that does not match the E.164 pattern
     * ({@code ^\+[1-9]\d{1,14}$}) must trigger bean validation and return HTTP 400
     * with a field-level error identifying {@code phoneNumber}. The phone number
     * stored in the DB must remain unchanged.
     */
    @Test
    @DisplayName("AC4 — PATCH /api/v1/profile with non-E.164 phoneNumber returns 400 with field-level validation error")
    void ac4_patchProfile_invalidPhone_returns400() throws Exception {
        String accessToken = getAccessToken(VALID_EMAIL, VALID_PASSWORD);

        String patchBody = """
                { "phoneNumber": "not-a-phone" }
                """;

        mockMvc.perform(patch(PROFILE_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(patchBody))
                .andExpect(status().isBadRequest())
                // GlobalExceptionHandler wraps bean validation errors in an "errors" array
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("phoneNumber"));

        // DB assertion — phone must NOT have been updated
        Customer saved = customerRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertThat(saved.getPhoneNumber())
                .as("phoneNumber must remain unchanged after a rejected PATCH attempt")
                .isEqualTo(VALID_PHONE);
    }

    // =========================================================================
    // AC5 — Profile changes are persisted and visible in the next GET
    // =========================================================================

    /**
     * AC5 — After a successful PATCH, a subsequent GET must reflect the updated
     * field values. This test verifies the full read-after-write cycle:
     * PATCH → HTTP response → GET → HTTP response → DB state.
     */
    @Test
    @DisplayName("AC5 — PATCH /api/v1/profile changes are persisted and returned by the next GET /api/v1/profile")
    void ac5_patchProfile_persistsChanges_visibleOnGet() throws Exception {
        String accessToken = getAccessToken(VALID_EMAIL, VALID_PASSWORD);

        // PATCH: update only firstName (partial update — other fields unchanged)
        String patchBody = """
                { "firstName": "Persisted" }
                """;

        mockMvc.perform(patch(PROFILE_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + accessToken)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Persisted"))
                // Unchanged fields must still be returned correctly
                .andExpect(jsonPath("$.lastName").value("Tester"))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE));

        // GET: the updated firstName must be visible in the next request
        mockMvc.perform(get(PROFILE_ENDPOINT)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Persisted"))
                .andExpect(jsonPath("$.lastName").value("Tester"))
                .andExpect(jsonPath("$.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.phoneNumber").value(VALID_PHONE));

        // DB assertion — the entity must have been saved with the new firstName
        Customer saved = customerRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertThat(saved.getFirstName())
                .as("firstName must be persisted in the database after a successful PATCH")
                .isEqualTo("Persisted");
        assertThat(saved.getLastName())
                .as("lastName must remain unchanged (only firstName was PATCHed)")
                .isEqualTo("Tester");
    }

    // =========================================================================
    // AC6 — Unauthenticated GET /api/v1/profile returns 401
    // =========================================================================

    /**
     * AC6 — A {@code GET /api/v1/profile} request without any {@code Authorization}
     * header must be rejected with HTTP 401 before reaching the controller.
     * The JWT filter writes the RFC 7807 response directly.
     */
    @Test
    @DisplayName("AC6 — GET /api/v1/profile without Authorization header returns 401")
    void ac6_getProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(PROFILE_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // AC6 — Unauthenticated PATCH /api/v1/profile returns 401
    // =========================================================================

    /**
     * AC6 — A {@code PATCH /api/v1/profile} request without any {@code Authorization}
     * header must be rejected with HTTP 401 before reaching the controller.
     */
    @Test
    @DisplayName("AC6 — PATCH /api/v1/profile without Authorization header returns 401")
    void ac6_patchProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch(PROFILE_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // AC6 — Malformed / invalid Bearer token returns 401
    // =========================================================================

    /**
     * AC6 — A structurally invalid JWT (not a valid Base64URL-encoded token)
     * supplied as the Bearer token must result in HTTP 401. The JWT filter catches
     * the {@code JwtException} thrown by JJWT's parser and writes the 401 directly.
     */
    @Test
    @DisplayName("AC6 — GET /api/v1/profile with malformed Bearer token returns 401")
    void ac6_getProfile_invalidToken_returns401() throws Exception {
        mockMvc.perform(get(PROFILE_ENDPOINT)
                        .header("Authorization", "Bearer invalid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // AC6 — SESSION token (wrong type) is rejected by the ACCESS-only filter
    // =========================================================================

    /**
     * AC6 — A valid SESSION JWT (issued by {@code POST /api/v1/auth/login}, with
     * {@code type="SESSION"}) must not be accepted as authentication for the profile
     * endpoints. The JWT filter calls {@code JwtConfig.validateAccessToken()}, which
     * checks {@code type == "ACCESS"} and throws {@code JwtException} for a SESSION
     * token, causing HTTP 401 to be returned.
     *
     * <p>Only the first authentication step (register + login) is executed here —
     * OTP verification is intentionally skipped because we only need the SESSION JWT,
     * not the ACCESS JWT.</p>
     */
    @Test
    @DisplayName("AC6 — GET /api/v1/profile with SESSION token (type=SESSION) returns 401 — wrong token type")
    void ac6_getProfile_sessionToken_returns401() throws Exception {
        // Setup: create customer directly in the repo (ACTIVE status, valid BCrypt hash)
        createActiveCustomer(VALID_EMAIL);

        // Step 1: Login → get the SESSION JWT only (OTP step intentionally skipped)
        String loginResponse = mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, VALID_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String sessionToken = objectMapper.readTree(loginResponse).get("sessionToken").asText();

        // Step 2: Use the SESSION token on a profile endpoint — must be rejected (AC6)
        // JwtConfig.validateAccessToken() enforces type == "ACCESS";
        // a SESSION token has type="SESSION" and is therefore rejected with 401.
        mockMvc.perform(get(PROFILE_ENDPOINT)
                        .header("Authorization", "Bearer " + sessionToken))
                .andExpect(status().isUnauthorized());
    }
}
