// Story: US-003
package com.northbank.registration.auth.otp;

import com.northbank.registration.auth.otp.dto.OtpVerifyResponse;
import com.northbank.registration.auth.otp.dto.ResendOtpResponse;
import com.northbank.registration.auth.otp.exception.InvalidOtpException;
import com.northbank.registration.auth.otp.exception.InvalidSessionTokenException;
import com.northbank.registration.auth.otp.exception.OtpSessionInvalidatedException;
import com.northbank.registration.auth.otp.exception.TooManyOtpRequestsException;
import com.northbank.registration.auth.sms.SmsService;
import com.northbank.registration.auth.token.RefreshToken;
import com.northbank.registration.auth.token.RefreshTokenRepository;
import com.northbank.registration.config.JwtConfig;
import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Core business logic for the SMS-OTP second authentication step (ADR-003).
 *
 * <h2>Flows:</h2>
 * <ul>
 *   <li>{@link #createOtpSession}: called by {@code AuthService.login()} on success —
 *       generates and persists the OTP, sends it via SMS stub.</li>
 *   <li>{@link #verifyOtp}: validates the submitted OTP, enforces lockout, issues
 *       access + refresh tokens on success (AC2–AC5).</li>
 *   <li>{@link #resendOtp}: rate-limited OTP resend with a 60-second cool-down.</li>
 * </ul>
 *
 * <h2>Transaction note:</h2>
 * <p>{@code verifyOtp} and {@code resendOtp} update the {@link OtpSession} row
 * (increment attempt counter, set invalidated flag) before throwing domain
 * exceptions. {@code noRollbackFor} ensures those state changes survive even
 * when an exception is raised — the same pattern as {@code AuthService.login()}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int    OTP_BOUND            = 1_000_000;
    private static final long   OTP_TTL_MINUTES      = 5L;
    private static final long   REFRESH_TOKEN_DAYS   = 7L;
    private static final int    REFRESH_TOKEN_BYTES  = 32;
    private static final int    MAX_FAILED_ATTEMPTS  = 3;
    private static final long   RESEND_COOL_DOWN_SEC = 60L;

    private final OtpSessionRepository  otpSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtConfig             jwtConfig;
    private final SmsService            smsService;
    private final CustomerRepository    customerRepository;
    private final PasswordEncoder       passwordEncoder;   // reserved for future use

    private final SecureRandom secureRandom = new SecureRandom();

    // ─────────────────────────────────────────────────────────────────────────
    // AC1 — Create OTP session after successful password check
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new OTP session immediately after a successful password check.
     *
     * <p>Called by {@code AuthService.login()} with the newly-generated
     * SESSION JWT string. Persists the session and fires an SMS stub.</p>
     *
     * @param customerId      UUID of the authenticated customer
     * @param rawSessionToken the raw compact JWT string just issued by login
     */
    @Transactional
    public void createOtpSession(UUID customerId, String rawSessionToken) {
        String otp              = String.format("%06d", secureRandom.nextInt(OTP_BOUND));
        String sessionTokenHash = sha256Hex(rawSessionToken);

        OtpSession session = OtpSession.builder()
                .customerId(customerId)
                .sessionTokenHash(sessionTokenHash)
                .otpCode(otp)
                .expiresAt(OffsetDateTime.now().plusMinutes(OTP_TTL_MINUTES))
                .failedAttempts(0)
                .invalidated(false)
                .build();
        otpSessionRepository.save(session);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Customer not found after login — id=" + customerId));
        smsService.sendOtp(customer.getPhoneNumber(), otp);

        log.info("OTP session created for customer id={}", customerId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AC2–AC5 — Verify OTP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifies the submitted OTP against the stored session.
     *
     * <p>On success issues access + refresh tokens and marks the session used.
     * On failure increments the attempt counter; locks the session after 3
     * consecutive failures (AC5).</p>
     *
     * @param rawSessionToken the SESSION JWT presented by the client
     * @param submittedOtp    the 6-digit OTP submitted by the customer
     * @return token pair on valid OTP (AC3)
     * @throws InvalidSessionTokenException  if the SESSION JWT is invalid/expired
     * @throws InvalidOtpException           if OTP is wrong or session is expired (AC2, AC4)
     * @throws OtpSessionInvalidatedException if the session was already invalidated (AC5)
     */
    @Transactional(noRollbackFor = {InvalidOtpException.class, OtpSessionInvalidatedException.class,
                                    InvalidSessionTokenException.class})
    public OtpVerifyResponse verifyOtp(String rawSessionToken, String submittedOtp) {

        // Step 1 — Validate SESSION JWT (signature, expiry, type claim)
        UUID customerId = jwtConfig.validateSessionToken(rawSessionToken);

        // Step 2 — Locate OTP session by hashed token
        String hash    = sha256Hex(rawSessionToken);
        OtpSession session = otpSessionRepository.findBySessionTokenHash(hash)
                .orElseThrow(() -> {
                    log.warn("OTP session not found for customer id={}", customerId);
                    return new InvalidOtpException(0);
                });

        // Step 3 — Session invalidated by prior lock-out or replay prevention
        if (session.isInvalidated()) {
            log.warn("Attempt against invalidated OTP session, customer id={}", customerId);
            throw new OtpSessionInvalidatedException();
        }

        // Step 4 — OTP window expired (not a user attempt — do not increment counter)
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            int remaining = Math.max(0, MAX_FAILED_ATTEMPTS - session.getFailedAttempts());
            log.warn("Expired OTP submitted for customer id={}", customerId);
            throw new InvalidOtpException(remaining);
        }

        // Step 5 — Wrong OTP: increment counter, lock if threshold reached
        if (!submittedOtp.equals(session.getOtpCode())) {
            int newAttempts = session.getFailedAttempts() + 1;
            session.setFailedAttempts(newAttempts);
            if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                session.setInvalidated(true);
                log.warn("OTP session invalidated after {} consecutive failures, customer id={}",
                        newAttempts, customerId);
            }
            otpSessionRepository.save(session);
            int remaining = Math.max(0, MAX_FAILED_ATTEMPTS - newAttempts);
            throw new InvalidOtpException(remaining);
        }

        // Step 6 — Valid OTP: generate access token + refresh token, consume session
        String accessToken   = jwtConfig.generateAccessToken(customerId);
        String rawRefresh    = generateOpaqueToken();
        String refreshHash   = sha256Hex(rawRefresh);

        RefreshToken refreshToken = RefreshToken.builder()
                .customerId(customerId)
                .tokenHash(refreshHash)
                .expiresAt(OffsetDateTime.now().plusDays(REFRESH_TOKEN_DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        // Consume the OTP session — prevents replay
        session.setInvalidated(true);
        otpSessionRepository.save(session);

        log.info("2FA complete — tokens issued for customer id={}", customerId);
        return new OtpVerifyResponse(accessToken, rawRefresh);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resend OTP (rate-limited)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resends the OTP to the customer's registered phone number.
     *
     * <p>The session must exist, not be invalidated, and at least 60 seconds
     * must have elapsed since it was last issued/resent.</p>
     *
     * @param rawSessionToken the SESSION JWT identifying the OTP session
     * @return confirmation message
     * @throws InvalidSessionTokenException   if the SESSION JWT is invalid/expired
     * @throws OtpSessionInvalidatedException if the session is invalidated
     * @throws TooManyOtpRequestsException    if resent within the 60-second cool-down
     */
    @Transactional(noRollbackFor = {OtpSessionInvalidatedException.class,
                                    TooManyOtpRequestsException.class,
                                    InvalidSessionTokenException.class})
    public ResendOtpResponse resendOtp(String rawSessionToken) {

        // Step 1 — Validate SESSION JWT
        UUID customerId = jwtConfig.validateSessionToken(rawSessionToken);

        // Step 2 — Locate OTP session
        String hash    = sha256Hex(rawSessionToken);
        OtpSession session = otpSessionRepository.findBySessionTokenHash(hash)
                .orElseThrow(() -> {
                    log.warn("OTP session not found on resend for customer id={}", customerId);
                    return new OtpSessionInvalidatedException();
                });

        if (session.isInvalidated()) {
            log.warn("Resend attempted on invalidated session, customer id={}", customerId);
            throw new OtpSessionInvalidatedException();
        }

        // Step 3 — Rate-limit: must wait RESEND_COOL_DOWN_SEC seconds
        OffsetDateTime earliest = session.getCreatedAt().plusSeconds(RESEND_COOL_DOWN_SEC);
        if (earliest.isAfter(OffsetDateTime.now())) {
            log.warn("OTP resend too soon for customer id={}", customerId);
            throw new TooManyOtpRequestsException();
        }

        // Step 4 — Generate new OTP, refresh expiry and reset counters
        String newOtp = String.format("%06d", secureRandom.nextInt(OTP_BOUND));
        session.setOtpCode(newOtp);
        session.setFailedAttempts(0);
        session.setExpiresAt(OffsetDateTime.now().plusMinutes(OTP_TTL_MINUTES));
        session.setCreatedAt(OffsetDateTime.now());   // resets the rate-limit window
        otpSessionRepository.save(session);

        // Step 5 — Send new OTP via SMS stub
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalStateException(
                        "Customer not found on resend — id=" + customerId));
        smsService.sendOtp(customer.getPhoneNumber(), newOtp);

        log.info("OTP resent for customer id={}", customerId);
        return new ResendOtpResponse("OTP sent");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Computes SHA-256 and returns the result as a 64-character lowercase hex string.
     *
     * @param input the raw string to hash (UTF-8 encoded)
     * @return 64-char hex digest
     */
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }

    /**
     * Generates a cryptographically secure opaque token (32 random bytes, Base64URL encoded).
     *
     * @return 43-character Base64URL string (no padding)
     */
    private String generateOpaqueToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
