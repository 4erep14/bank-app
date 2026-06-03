// Story: US-001 / US-002 / US-003 / US-004 / US-005
package com.northbank.registration.shared.exception;

import com.northbank.registration.auth.exception.InvalidResetTokenException;
import com.northbank.registration.auth.login.exception.AccountLockedException;
import com.northbank.registration.auth.login.exception.InvalidCredentialsException;
import com.northbank.registration.auth.otp.exception.InvalidOtpException;
import com.northbank.registration.auth.otp.exception.InvalidSessionTokenException;
import com.northbank.registration.auth.otp.exception.OtpSessionInvalidatedException;
import com.northbank.registration.auth.otp.exception.TooManyOtpRequestsException;
import com.northbank.registration.customer.exception.EmailAlreadyRegisteredException;
import com.northbank.registration.profile.exception.FieldNotEditableException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Global RFC 7807 Problem Details exception handler (ADR-001 / ADR-002 / ADR-003).
 *
 * <p>Handles:</p>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException}     → 400 with {@code errors[]} array</li>
 *   <li>{@link FieldNotEditableException}            → 400 "Field is not editable" (US-005 AC3)</li>
 *   <li>{@link HttpMessageNotReadableException}      → 400, unwraps {@link FieldNotEditableException} (defensive, US-005 AC3)</li>
 *   <li>{@link InvalidResetTokenException}          → 400 (US-004)</li>
 *   <li>{@link EmailAlreadyRegisteredException}     → 409 (US-001)</li>
 *   <li>{@link DataIntegrityViolationException}     → 409 on unique-email constraint</li>
 *   <li>{@link InvalidCredentialsException}         → 401 (US-002 AC3)</li>
 *   <li>{@link AccountLockedException}              → 423 (US-002 AC4/AC5)</li>
 *   <li>{@link InvalidSessionTokenException}        → 401 (US-003)</li>
 *   <li>{@link OtpSessionInvalidatedException}      → 401 (US-003 AC5)</li>
 *   <li>{@link InvalidOtpException}                 → 401 + remainingAttempts (US-003 AC4)</li>
 *   <li>{@link TooManyOtpRequestsException}         → 429 + Retry-After: 60 (US-003)</li>
 *   <li>{@link Exception} catch-all                 → 500</li>
 * </ul>
 *
 * <p>The {@code password} field is NEVER written to any log line or response body.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEMS_BASE_URI        = "https://api.bank.example/problems/";
    private static final String VALIDATION_TYPE           = PROBLEMS_BASE_URI + "validation-error";
    private static final String EMAIL_CONFLICT_TYPE       = PROBLEMS_BASE_URI + "email-already-registered";
    private static final String INVALID_CREDENTIALS_TYPE  = PROBLEMS_BASE_URI + "invalid-credentials";
    private static final String ACCOUNT_LOCKED_TYPE       = PROBLEMS_BASE_URI + "account-locked";
    private static final String INVALID_RESET_TOKEN_TYPE  = PROBLEMS_BASE_URI + "invalid-reset-token";
    private static final String INVALID_SESSION_TOKEN_TYPE = PROBLEMS_BASE_URI + "invalid-session-token";
    private static final String OTP_INVALID_TYPE          = PROBLEMS_BASE_URI + "invalid-otp";
    private static final String OTP_SESSION_INVALID_TYPE  = PROBLEMS_BASE_URI + "otp-session-invalidated";
    private static final String TOO_MANY_OTP_REQUESTS_TYPE = PROBLEMS_BASE_URI + "too-many-otp-requests";
    private static final String FIELD_NOT_EDITABLE_TYPE   = PROBLEMS_BASE_URI + "field-not-editable";  // US-005

    // ─── 400 — Bean Validation failure ───────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorMap)
                .toList();

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more fields are invalid."
        );
        pd.setType(URI.create(VALIDATION_TYPE));
        pd.setTitle("Validation Failed");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("errors", errors);

        log.debug("Validation failed for request to {}: {} error(s)", request.getRequestURI(), errors.size());
        return pd;
    }

    // ─── 400 — Read-only field update attempt (US-005 AC3) ───────────────────

    /**
     * Returns 400 when a client tries to update a read-only profile field
     * ({@code email} or {@code dateOfBirth}) via {@code PATCH /api/v1/profile}.
     *
     * <p>This handler fires when {@code @JsonAnySetter} on {@link
     * com.northbank.registration.profile.UpdateProfileRequest} throws
     * {@link FieldNotEditableException} during Jackson deserialization. Jackson
     * re-throws RuntimeExceptions from {@code @JsonAnySetter} without wrapping,
     * so Spring MVC routes it directly to this handler.</p>
     *
     * <p>AC3 exact wording: {@code "Field is not editable"} — the {@code detail}
     * value is fixed so frontend and AQA tests can assert on it precisely.</p>
     */
    @ExceptionHandler(FieldNotEditableException.class)
    public ResponseEntity<ProblemDetail> handleFieldNotEditable(
            FieldNotEditableException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Field is not editable"
        );
        pd.setType(URI.create(FIELD_NOT_EDITABLE_TYPE));
        pd.setTitle("Field Not Editable");
        pd.setInstance(URI.create(request.getRequestURI()));

        log.warn("Attempt to update read-only field '{}' at URI: {}", ex.getFieldName(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    /**
     * Defensive handler for {@link HttpMessageNotReadableException} that checks
     * whether the root cause is a {@link FieldNotEditableException}.
     *
     * <p>In some Jackson versions the RuntimeException thrown by
     * {@code @JsonAnySetter} may be wrapped in a Jackson exception before Spring
     * wraps it in {@link HttpMessageNotReadableException}.  This handler ensures
     * AC3 is enforced consistently regardless of Jackson version.</p>
     *
     * <p>For any other {@link HttpMessageNotReadableException} (e.g. malformed JSON)
     * a generic 400 is returned.</p>
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        // Unwrap cause chain looking for FieldNotEditableException
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof FieldNotEditableException fne) {
                return handleFieldNotEditable(fne, request);
            }
            cause = cause.getCause();
        }

        // Generic malformed-body 400
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or cannot be parsed."
        );
        pd.setType(URI.create(VALIDATION_TYPE));
        pd.setTitle("Malformed Request Body");
        pd.setInstance(URI.create(request.getRequestURI()));

        log.debug("Malformed request body at URI {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    // ─── 400 — Invalid / expired reset token (US-004) ────────────────────────
    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidResetToken(
            InvalidResetTokenException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        pd.setType(URI.create(INVALID_RESET_TOKEN_TYPE));
        pd.setTitle("Invalid Reset Token");
        pd.setInstance(URI.create(request.getRequestURI()));

        log.warn("Invalid reset token used at URI: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    // ─── 401 — Invalid credentials (US-002 AC3) ──────────────────────────────

    /**
     * Returns 401 with a generic "Invalid email or password" message.
     * Intentionally does not distinguish between unknown email and wrong password
     * to prevent user-enumeration attacks.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
        );
        pd.setType(URI.create(INVALID_CREDENTIALS_TYPE));
        pd.setTitle("Unauthorized");
        pd.setInstance(URI.create(request.getRequestURI()));

        log.warn("Invalid credentials attempt at URI: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    // ─── 423 — Account locked (US-002 AC4 / AC5) ─────────────────────────────

    /**
     * Returns 423 Locked.
     * Triggered when a login is attempted against a LOCKED account, or when the
     * 5th consecutive failed attempt transitions the account to LOCKED.
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ProblemDetail> handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.LOCKED,
                "Account locked due to too many failed login attempts"
        );
        pd.setType(URI.create(ACCOUNT_LOCKED_TYPE));
        pd.setTitle("Account Locked");
        pd.setInstance(URI.create(request.getRequestURI()));

        log.warn("Locked account login attempt at URI: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.LOCKED).body(pd);
    }

    // ─── 409 — Friendly duplicate-email pre-check (US-001) ───────────────────

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Email already registered"
        );
        pd.setType(URI.create(EMAIL_CONFLICT_TYPE));
        pd.setTitle("Email Already Registered");
        pd.setInstance(URI.create(request.getRequestURI()));

        log.warn("Registration rejected — duplicate email, request URI: {}", request.getRequestURI());
        return pd;
    }

    // ─── 409 — Race-safe: DB unique constraint violation (US-001) ────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        // uq_customers_email is the only unique constraint on this table for US-001
        if (isEmailUniqueConstraintViolation(ex)) {
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT,
                    "Email already registered"
            );
            pd.setType(URI.create(EMAIL_CONFLICT_TYPE));
            pd.setTitle("Email Already Registered");
            pd.setInstance(URI.create(request.getRequestURI()));
            log.warn("DB unique-constraint violation on customers.email, URI: {}", request.getRequestURI());
            return pd;
        }

        // For any other integrity violation fall through to the 500 handler
        log.error("Unexpected DataIntegrityViolationException for URI {}", request.getRequestURI(), ex);
        return buildServerError(request);
    }

    // ─── 415 — Unsupported Content-Type ──────────────────────────────────────

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type '" + ex.getContentType() + "' is not supported. Use 'application/json'."
        );
        pd.setTitle("Unsupported Media Type");
        pd.setInstance(URI.create(request.getRequestURI()));
        log.debug("Unsupported media type '{}' for URI {}", ex.getContentType(), request.getRequestURI());
        return pd;
    }

    // ─── 401 — Invalid session token (US-003) ────────────────────────────────

    /**
     * Returns 401 when the SESSION JWT is missing, structurally invalid,
     * expired, or does not carry {@code type="SESSION"}.
     */
    @ExceptionHandler(InvalidSessionTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidSessionToken(
            InvalidSessionTokenException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        pd.setType(URI.create(INVALID_SESSION_TOKEN_TYPE));
        pd.setTitle("Invalid Session Token");
        pd.setInstance(URI.create(request.getRequestURI()));

        log.warn("Invalid session token at URI: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    // ─── 401 — OTP session invalidated (US-003 AC5) ──────────────────────────

    /**
     * Returns 401 when the OTP session has been invalidated by 3 consecutive
     * failed attempts or has already been consumed.
     */
    @ExceptionHandler(OtpSessionInvalidatedException.class)
    public ResponseEntity<ProblemDetail> handleOtpSessionInvalidated(
            OtpSessionInvalidatedException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        pd.setType(URI.create(OTP_SESSION_INVALID_TYPE));
        pd.setTitle("OTP Session Invalidated");
        pd.setInstance(URI.create(request.getRequestURI()));

        log.warn("Invalidated OTP session used at URI: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    // ─── 401 — Invalid/expired OTP with remainingAttempts (US-003 AC2/AC4) ───

    /**
     * Returns 401 with the {@code remainingAttempts} extension field.
     * Used for both wrong OTP (AC4) and expired OTP window (AC2).
     */
    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ProblemDetail> handleInvalidOtp(
            InvalidOtpException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage()
        );
        pd.setType(URI.create(OTP_INVALID_TYPE));
        pd.setTitle("Invalid or Expired OTP");
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("remainingAttempts", ex.getRemainingAttempts());

        log.warn("Invalid OTP at URI {} — remainingAttempts={}",
                request.getRequestURI(), ex.getRemainingAttempts());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    // ─── 429 — OTP resend rate limit (US-003) ────────────────────────────────

    /**
     * Returns 429 Too Many Requests with a {@code Retry-After: 60} header.
     */
    @ExceptionHandler(TooManyOtpRequestsException.class)
    public ResponseEntity<ProblemDetail> handleTooManyOtpRequests(
            TooManyOtpRequestsException ex,
            HttpServletRequest request) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                ex.getMessage()
        );
        pd.setType(URI.create(TOO_MANY_OTP_REQUESTS_TYPE));
        pd.setTitle("Too Many OTP Requests");
        pd.setInstance(URI.create(request.getRequestURI()));

        log.warn("OTP resend rate limit hit at URI: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", "60")
                .body(pd);
    }

    // ─── Catch-all → 500 ─────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAll(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for URI {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildServerError(request);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Map<String, String> toErrorMap(FieldError fe) {
        return Map.of(
                "field",   fe.getField(),
                "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
        );
    }

    private boolean isEmailUniqueConstraintViolation(DataIntegrityViolationException ex) {
        String msg = ex.getMostSpecificCause().getMessage();
        return msg != null && msg.contains("uq_customers_email");
    }

    private ProblemDetail buildServerError(HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later."
        );
        pd.setTitle("Internal Server Error");
        pd.setInstance(URI.create(request.getRequestURI()));
        return pd;
    }
}
