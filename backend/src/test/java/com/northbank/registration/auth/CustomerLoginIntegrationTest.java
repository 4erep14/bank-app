// Story: US-002
package com.northbank.registration.auth;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for US-002: Customer Login.
 *
 * <p>Acceptance Criteria covered:</p>
 * <ul>
 *   <li>AC1 — Customer can submit login with email and password via
 *       {@code POST /api/v1/auth/login}</li>
 *   <li>AC2 — On valid credentials the system returns 200 with
 *       {@code { "status": "2FA_REQUIRED", "sessionToken": "<temp-token>" }}</li>
 *   <li>AC3 — On invalid credentials the API returns 401 with
 *       message "Invalid email or password"</li>
 *   <li>AC4 — After 5 consecutive failed login attempts the account status is
 *       set to {@code LOCKED} and the API returns 423 with message
 *       "Account locked due to too many failed login attempts"</li>
 *   <li>AC5 — A {@code LOCKED} account cannot log in (returns 423 immediately)</li>
 * </ul>
 *
 * <p><strong>Transaction / data-isolation note:</strong> {@link IntegrationTestBase}
 * is {@code @Transactional}. All MockMvc calls (via {@code TestDispatcherServlet})
 * run synchronously on the same test thread and therefore JOIN the outer test
 * transaction. Changes written by the application's {@code @Transactional} service
 * layer are immediately visible to subsequent repository reads in the same test.
 * The transaction is always rolled back after each test, so tests are fully
 * isolated from one another.</p>
 *
 * <p>{@code AuthService} uses
 * {@code @Transactional(noRollbackFor = {InvalidCredentialsException.class,
 * AccountLockedException.class})} to prevent the lockout writes from being
 * undone mid-call when an exception is thrown. In the test context this means
 * the incremented counters and {@code LOCKED} status remain visible within the
 * test transaction for DB-state assertions.</p>
 */
@DisplayName("US-002 — Customer Login Integration Tests")
class CustomerLoginIntegrationTest extends IntegrationTestBase {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";

    /** Canonical test email — lower-cased, unique per test (cleaned by @BeforeEach). */
    private static final String VALID_EMAIL    = "login.test@example.com";

    /** Password satisfying the application's complexity rule (≥8, upper, lower, digit, special). */
    private static final String VALID_PASSWORD = "Password1!";

    /** A password that does NOT match VALID_PASSWORD's BCrypt hash. */
    private static final String WRONG_PASSWORD = "WrongP4ss@";

    // ── Test infrastructure ───────────────────────────────────────────────────

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Real {@link PasswordEncoder} bean (BCrypt strength 12).
     * Injected so tests can produce valid BCrypt hashes for direct-DB fixture setup,
     * matching exactly the hashes that {@code AuthService} will verify at runtime.
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    // ── Setup / teardown ─────────────────────────────────────────────────────

    /**
     * Wipes all customer rows before every test.
     *
     * <p>Because {@link IntegrationTestBase} is {@code @Transactional}, each test
     * runs in a transaction that is rolled back afterwards. The {@code deleteAll()}
     * here acts as a safety net for any data committed by a previous test run (e.g.
     * if a future test is annotated with {@code @Commit}).</p>
     */
    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds and persists a customer with {@code ACTIVE} status and a real BCrypt
     * hash for the supplied raw password.
     *
     * <p>Runs in the current test transaction, so it is immediately visible to
     * any subsequent MockMvc call on the same thread.</p>
     */
    private Customer createActiveCustomer(String email, String rawPassword) {
        Customer customer = Customer.builder()
                .firstName("Login")
                .lastName("Tester")
                .email(email.toLowerCase())
                .phoneNumber("+14155552671")
                .dateOfBirth(LocalDate.of(1990, 6, 15))
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(CustomerStatus.ACTIVE)
                // failedLoginAttempts defaults to 0 via @Builder.Default
                .build();
        return customerRepository.save(customer);
    }

    /** Builds a valid JSON login request body. */
    private String loginBody(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    // =========================================================================
    // AC1 — POST /api/v1/auth/login endpoint is reachable
    // =========================================================================

    /**
     * AC1 — Verifies that the endpoint is wired at the correct path and HTTP method,
     * is declared public (no authentication required), and accepts
     * {@code application/json} bodies. A valid request must return 200, not 404/405.
     */
    @Test
    @DisplayName("AC1 — POST /api/v1/auth/login is reachable, public, and accepts JSON")
    void ac1_loginEndpoint_isReachable() throws Exception {
        createActiveCustomer(VALID_EMAIL, VALID_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, VALID_PASSWORD)))
                // 200 proves: correct path, correct method, public (no auth required),
                // JSON body accepted — all four elements of AC1 covered.
                .andExpect(status().isOk());
    }

    // =========================================================================
    // AC2 — Valid credentials → 200 with 2FA_REQUIRED + non-blank sessionToken
    // =========================================================================

    /**
     * AC2 — Happy path: valid email + password returns HTTP 200 with
     * {@code status="2FA_REQUIRED"} and a non-blank {@code sessionToken}.
     * The token is a 5-minute JWT — we assert it is non-blank, not its content.
     */
    @Test
    @DisplayName("AC2 — Valid credentials return 200 with status=2FA_REQUIRED and non-blank sessionToken")
    void ac2_validCredentials_returns2faRequired() throws Exception {
        createActiveCustomer(VALID_EMAIL, VALID_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, VALID_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("2FA_REQUIRED"))
                .andExpect(jsonPath("$.sessionToken").isNotEmpty())
                // password / hash must NEVER leak into the response
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    /**
     * AC2 (extended) — Email matching is case-insensitive: upper-cased email in
     * the login request is normalised to lower-case before lookup and still succeeds.
     */
    @Test
    @DisplayName("AC2 — Login succeeds when email is submitted in mixed case")
    void ac2_emailCaseInsensitive_returnsOk() throws Exception {
        createActiveCustomer(VALID_EMAIL, VALID_PASSWORD);

        // Submit email in upper-case — service normalises to lower-case
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL.toUpperCase(), VALID_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("2FA_REQUIRED"));
    }

    /**
     * AC2 + AC4 (counter reset) — A successful login after prior failures resets
     * the {@code failed_login_attempts} counter to 0 in the database.
     */
    @Test
    @DisplayName("AC4 — Successful login after failures resets failedLoginAttempts counter to 0 in DB")
    void ac4_successAfterFailures_resetsCounter() throws Exception {
        // Pre-load customer with 3 prior failed attempts
        Customer customer = createActiveCustomer(VALID_EMAIL, VALID_PASSWORD);
        customer.setFailedLoginAttempts(3);
        customerRepository.save(customer);

        // Successful login
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, VALID_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("2FA_REQUIRED"));

        // DB assertion: counter must be reset
        Customer afterLogin = customerRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertThat(afterLogin.getFailedLoginAttempts())
                .as("failedLoginAttempts must be reset to 0 after successful login")
                .isZero();
    }

    // =========================================================================
    // AC3 — Invalid credentials → 401 with RFC 7807 ProblemDetail
    // =========================================================================

    /**
     * AC3 — Wrong password for a known email returns 401 with the exact
     * RFC 7807 {@code detail} message "Invalid email or password".
     */
    @Test
    @DisplayName("AC3 — Wrong password returns 401 with 'Invalid email or password' detail")
    void ac3_invalidPassword_returns401() throws Exception {
        createActiveCustomer(VALID_EMAIL, VALID_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, WRONG_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    /**
     * AC3 — Unknown email returns 401 with the same generic message.
     * The system must NOT distinguish "user not found" from "wrong password"
     * to prevent user-enumeration attacks.
     */
    @Test
    @DisplayName("AC3 — Unknown email returns 401 with 'Invalid email or password' (no user enumeration)")
    void ac3_unknownEmail_returns401() throws Exception {
        // No customer registered — any email must return 401, never 404
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("nobody@example.com", VALID_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    /**
     * AC3 (DB-state) — Each failed login attempt increments the
     * {@code failed_login_attempts} counter in the database.
     */
    @Test
    @DisplayName("AC3 — Failed login increments failedLoginAttempts counter in DB")
    void ac3_invalidPassword_incrementsFailedLoginAttemptsInDb() throws Exception {
        createActiveCustomer(VALID_EMAIL, VALID_PASSWORD);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, WRONG_PASSWORD)))
                .andExpect(status().isUnauthorized());

        Customer afterFailure = customerRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertThat(afterFailure.getFailedLoginAttempts())
                .as("failedLoginAttempts must be incremented to 1 after first failed attempt")
                .isEqualTo(1);
        assertThat(afterFailure.getStatus())
                .as("Account must remain ACTIVE — not yet locked after 1 failure")
                .isEqualTo(CustomerStatus.ACTIVE);
    }

    /**
     * AC3 (validation) — Requests with a blank email field return 400 (Bean Validation),
     * not 401 — the validation gate fires before business logic.
     */
    @Test
    @DisplayName("AC3 — Blank email in request body returns 400 (Bean Validation), not 401")
    void ac3_blankEmail_returns400() throws Exception {
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "", "password": "Password1!" }
                                """))
                .andExpect(status().isBadRequest());
    }

    /**
     * AC3 (validation) — Requests with a blank password field return 400,
     * not 401 — validation fires first.
     */
    @Test
    @DisplayName("AC3 — Blank password in request body returns 400 (Bean Validation), not 401")
    void ac3_blankPassword_returns400() throws Exception {
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "test@example.com", "password": "" }
                                """))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // AC4 — 5 consecutive failures → account LOCKED + 423
    // =========================================================================

    /**
     * AC4 — Full lockout flow:
     * <ol>
     *   <li>Attempts 1–4 return 401 — account still ACTIVE.</li>
     *   <li>Attempt 5 returns 423 — lockout triggered.</li>
     *   <li>DB assertions: {@code status=LOCKED}, {@code failedLoginAttempts=5},
     *       {@code lockedAt} is not null.</li>
     * </ol>
     *
     * <p>All 5 MockMvc calls share the test's outer transaction, so each
     * {@code save()} inside {@code AuthService} is immediately visible to
     * subsequent {@code findByEmail()} calls within the same transaction.</p>
     */
    @Test
    @DisplayName("AC4 — Five consecutive wrong-password attempts lock the account: 5th returns 423, DB shows LOCKED")
    void ac4_fiveFailedAttempts_locksAccount() throws Exception {
        createActiveCustomer(VALID_EMAIL, VALID_PASSWORD);

        // Attempts 1–4: each must return 401
        for (int attempt = 1; attempt <= 4; attempt++) {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(VALID_EMAIL, WRONG_PASSWORD)))
                    .andExpect(status().isUnauthorized());
        }

        // Attempt 5: the threshold is reached → 423 Locked
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, WRONG_PASSWORD)))
                .andExpect(status().is(423))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(423))
                .andExpect(jsonPath("$.detail")
                        .value("Account locked due to too many failed login attempts"));

        // ── DB-state assertions (AC4 core requirement) ────────────────────────
        Customer locked = customerRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertThat(locked.getStatus())
                .as("Customer status must be LOCKED after 5 consecutive failures")
                .isEqualTo(CustomerStatus.LOCKED);
        assertThat(locked.getFailedLoginAttempts())
                .as("failedLoginAttempts must be exactly 5 after 5 failed attempts")
                .isEqualTo(5);
        assertThat(locked.getLockedAt())
                .as("lockedAt timestamp must be set when account is locked")
                .isNotNull();
    }

    /**
     * AC4 (boundary) — Four consecutive failures do NOT lock the account.
     * Only the 5th attempt triggers the lockout.
     */
    @Test
    @DisplayName("AC4 — Four consecutive failures do NOT lock the account (boundary check)")
    void ac4_fourFailedAttempts_doesNotLockAccount() throws Exception {
        createActiveCustomer(VALID_EMAIL, VALID_PASSWORD);

        for (int attempt = 1; attempt <= 4; attempt++) {
            mockMvc.perform(post(LOGIN_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody(VALID_EMAIL, WRONG_PASSWORD)))
                    .andExpect(status().isUnauthorized());
        }

        // After 4 failures the account must still be ACTIVE
        Customer customer = customerRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertThat(customer.getStatus())
                .as("Account must remain ACTIVE after only 4 failed attempts")
                .isEqualTo(CustomerStatus.ACTIVE);
        assertThat(customer.getFailedLoginAttempts())
                .as("failedLoginAttempts must be 4 after 4 failed attempts")
                .isEqualTo(4);
        assertThat(customer.getLockedAt())
                .as("lockedAt must remain null — account not yet locked")
                .isNull();
    }

    // =========================================================================
    // AC5 — Pre-locked account → 423 immediately (no password evaluation)
    // =========================================================================

    /**
     * AC5 — A customer whose {@code status} is already {@code LOCKED} receives
     * HTTP 423 immediately, even when the correct password is supplied.
     * The lockout check happens BEFORE the BCrypt comparison (ADR-002).
     */
    @Test
    @DisplayName("AC5 — LOCKED account returns 423 immediately even with correct password")
    void ac5_lockedAccount_returns423Immediately() throws Exception {
        // Pre-lock the account directly in the DB — bypasses the 5-attempt flow
        persistLockedCustomer(VALID_EMAIL, VALID_PASSWORD);

        // Correct password — still 423
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, VALID_PASSWORD)))
                .andExpect(status().is(423))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(423))
                .andExpect(jsonPath("$.detail")
                        .value("Account locked due to too many failed login attempts"));
    }

    /**
     * AC5 — A locked account returns 423 even with a wrong password.
     * The lockout state takes precedence — the system should not reveal whether
     * the password is right or wrong when the account is locked.
     */
    @Test
    @DisplayName("AC5 — LOCKED account returns 423 even with wrong password (no credential disclosure)")
    void ac5_lockedAccount_wrongPassword_returns423() throws Exception {
        persistLockedCustomer(VALID_EMAIL, VALID_PASSWORD);

        // Wrong password — must still be 423, not 401
        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, WRONG_PASSWORD)))
                .andExpect(status().is(423))
                .andExpect(jsonPath("$.detail")
                        .value("Account locked due to too many failed login attempts"));
    }

    /**
     * AC5 (DB invariant) — The {@code lockedAt} timestamp is non-null and
     * {@code failedLoginAttempts} is 5 for a pre-locked customer; a subsequent
     * 423 response does NOT reset or alter those values.
     */
    @Test
    @DisplayName("AC5 — Login attempt against LOCKED account does not alter DB lockout fields")
    void ac5_lockedAccount_dbStateUnchangedAfterAttempt() throws Exception {
        Customer preLocked = persistLockedCustomer(VALID_EMAIL, VALID_PASSWORD);
        OffsetDateTime originalLockedAt = preLocked.getLockedAt();

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(VALID_EMAIL, VALID_PASSWORD)))
                .andExpect(status().is(423));

        // DB state must be unchanged
        Customer afterAttempt = customerRepository.findByEmail(VALID_EMAIL).orElseThrow();
        assertThat(afterAttempt.getStatus())
                .as("Status must remain LOCKED")
                .isEqualTo(CustomerStatus.LOCKED);
        assertThat(afterAttempt.getFailedLoginAttempts())
                .as("failedLoginAttempts must not be incremented past 5 for a LOCKED account")
                .isEqualTo(5);
        assertThat(afterAttempt.getLockedAt())
                .as("lockedAt must not change on subsequent login attempts against a LOCKED account")
                .isEqualTo(originalLockedAt);
    }

    // ── Private fixture helpers ───────────────────────────────────────────────

    /**
     * Builds and persists a customer that is already in the {@code LOCKED} state
     * with {@code failedLoginAttempts=5} and a populated {@code lockedAt} timestamp.
     * Runs in the current test transaction.
     */
    private Customer persistLockedCustomer(String email, String rawPassword) {
        Customer customer = Customer.builder()
                .firstName("Locked")
                .lastName("User")
                .email(email.toLowerCase())
                .phoneNumber("+14155552671")
                .dateOfBirth(LocalDate.of(1985, 3, 20))
                .passwordHash(passwordEncoder.encode(rawPassword))
                .status(CustomerStatus.LOCKED)
                .failedLoginAttempts(5)
                .lockedAt(OffsetDateTime.now().minusMinutes(1))
                .build();
        return customerRepository.save(customer);
    }
}
