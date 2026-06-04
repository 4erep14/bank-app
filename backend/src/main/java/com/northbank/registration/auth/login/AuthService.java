// Story: US-002 / US-003
package com.northbank.registration.auth.login;

import com.northbank.registration.auth.login.dto.LoginRequest;
import com.northbank.registration.auth.login.dto.LoginResponse;
import com.northbank.registration.auth.login.exception.AccountLockedException;
import com.northbank.registration.auth.login.exception.InvalidCredentialsException;
import com.northbank.registration.auth.otp.OtpService;
import com.northbank.registration.audit.domain.model.AuditActionType;
import com.northbank.registration.audit.service.AuditLogService;
import com.northbank.registration.config.JwtConfig;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Business-logic service for the first authentication step (ADR-002).
 *
 * <h2>Account lockout algorithm (atomic {@code @Transactional}):</h2>
 * <ol>
 *   <li>Look up customer by normalised (lowercase-trimmed) email.
 *       Not found → 401 {@link InvalidCredentialsException}.</li>
 *   <li>If {@code status = LOCKED} → 423 {@link AccountLockedException} immediately.</li>
 *   <li>Verify password with BCrypt:
 *     <ul>
 *       <li>Match → reset {@code failedLoginAttempts = 0}, create OTP session (US-003),
 *           return 200 with sessionToken.</li>
 *       <li>No match → increment {@code failedLoginAttempts}.
 *           If ≥ 5 → set {@code status = LOCKED}, {@code lockedAt = now()}, 423.
 *           Else → 401.</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <p><strong>Transaction note:</strong> {@code noRollbackFor} is set on the two
 * domain exceptions so the counter/lock updates are committed to the DB even when
 * an exception is thrown — ensuring lockout state is durable.</p>
 *
 * <p><strong>US-003 additive change:</strong> After a successful password check,
 * {@link OtpService#createOtpSession(java.util.UUID, String)} is called with the
 * newly-generated SESSION JWT. The ADR-002 response contract is unchanged.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final String SESSION_STATUS    = "2FA_REQUIRED";

    private final CustomerRepository customerRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtConfig           jwtConfig;
    private final AuditLogService     auditLogService;

    /**
     * OtpService wired at construction time.
     * No circular dependency — OtpService never injects AuthService.
     */
    private final OtpService otpService;

    /**
     * Processes a login attempt.
     *
     * @param request validated login request (email + password)
     * @return {@link LoginResponse} with {@code status="2FA_REQUIRED"} and a signed JWT
     * @throws InvalidCredentialsException if email is unknown or password is wrong (AC3)
     * @throws AccountLockedException      if the account is already locked (AC5) or becomes
     *                                     locked after this attempt (AC4)
     */
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountLockedException.class})
    public LoginResponse login(LoginRequest request) {

        // Step 1 — Normalise email and find customer (not found → 401)
        String email = request.email().toLowerCase().trim();
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login attempt for unknown email domain: {}", sanitiseEmailForLog(email));
                    return new InvalidCredentialsException();
                });

        // Step 2 — Already locked → 423 (AC5)
        if (customer.getStatus() == CustomerStatus.LOCKED) {
            log.warn("Login attempt against LOCKED account id={}", customer.getId());
            throw new AccountLockedException();
        }

        // Step 3 — Verify password
        if (passwordEncoder.matches(request.password(), customer.getPasswordHash())) {
            // Match: reset counter and issue sessionToken (AC2)
            customer.setFailedLoginAttempts(0);
            customerRepository.save(customer);

            String sessionToken = jwtConfig.generateSessionToken(customer.getId());

            // US-003 additive: create OTP session and fire SMS stub
            otpService.createOtpSession(customer.getId(), sessionToken);
            auditLogService.record(
                    AuditActionType.LOGIN_SUCCESS,
                    customer.getId(),
                    customer.getRole().name(),
                    "CUSTOMER",
                    customer.getId());

            log.info("Login step-1 success for customer id={}", customer.getId());
            return new LoginResponse(SESSION_STATUS, sessionToken);

        } else {
            // No match: increment counter
            int attempts = customer.getFailedLoginAttempts() + 1;
            customer.setFailedLoginAttempts(attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                // Threshold reached → lock account (AC4)
                customer.setStatus(CustomerStatus.LOCKED);
                customer.setLockedAt(OffsetDateTime.now());
                customerRepository.save(customer);
                auditLogService.record(
                        AuditActionType.LOGIN_FAILURE,
                        customer.getId(),
                        customer.getRole().name(),
                        "CUSTOMER",
                        customer.getId());
                log.warn("Account id={} LOCKED after {} consecutive failed login attempts",
                        customer.getId(), attempts);
                throw new AccountLockedException();
            }

            // Below threshold → persist incremented counter and return 401 (AC3)
            customerRepository.save(customer);
            auditLogService.record(
                    AuditActionType.LOGIN_FAILURE,
                    customer.getId(),
                    customer.getRole().name(),
                    "CUSTOMER",
                    customer.getId());
            log.warn("Failed login attempt {}/{} for customer id={}",
                    attempts, MAX_FAILED_ATTEMPTS, customer.getId());
            throw new InvalidCredentialsException();
        }
    }

    /** Returns only the domain part of an email for safe logging — avoids PII. */
    private String sanitiseEmailForLog(String email) {
        if (email == null) return "unknown";
        int atIdx = email.indexOf('@');
        return atIdx >= 0 ? "*@" + email.substring(atIdx + 1) : "unknown";
    }
}
