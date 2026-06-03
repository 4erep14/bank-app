// Story: US-003
package com.northbank.registration.auth.otp.exception;

/**
 * Thrown when the submitted OTP is wrong, or when the OTP session has expired
 * (AC2, AC4). Carries the remaining attempt count for the RFC 7807 extension field.
 *
 * <p>Maps to HTTP 401 via {@code GlobalExceptionHandler} with a
 * {@code remainingAttempts} property in the ProblemDetail extensions.</p>
 */
public class InvalidOtpException extends RuntimeException {

    private final int remainingAttempts;

    public InvalidOtpException(int remainingAttempts) {
        super("Invalid or expired OTP");
        this.remainingAttempts = remainingAttempts;
    }

    /** How many more attempts the client may make before the session is invalidated. */
    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}
