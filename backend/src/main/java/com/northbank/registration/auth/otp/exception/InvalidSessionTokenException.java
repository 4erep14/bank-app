// Story: US-003
package com.northbank.registration.auth.otp.exception;

/**
 * Thrown when a SESSION JWT presented to {@code OtpService} is structurally
 * invalid, has an invalid signature, is expired, or does not carry the
 * required {@code type="SESSION"} claim.
 *
 * <p>Maps to HTTP 401 via {@code GlobalExceptionHandler}.</p>
 */
public class InvalidSessionTokenException extends RuntimeException {

    public InvalidSessionTokenException() {
        super("Invalid or expired session token. Please restart the login process.");
    }

    public InvalidSessionTokenException(String message) {
        super(message);
    }
}
