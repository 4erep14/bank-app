// Story: US-003
package com.northbank.registration.auth.otp.exception;

/**
 * Thrown when an OTP verification attempt is made against a session that has
 * been invalidated — either by 3 consecutive wrong OTP attempts (AC5) or
 * because the session was already consumed by a prior successful verify.
 *
 * <p>Maps to HTTP 401 via {@code GlobalExceptionHandler}.</p>
 */
public class OtpSessionInvalidatedException extends RuntimeException {

    public OtpSessionInvalidatedException() {
        super("OTP session is no longer valid. Please restart the login process.");
    }
}
