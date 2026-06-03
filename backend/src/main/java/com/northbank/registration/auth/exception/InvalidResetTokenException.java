// Story: US-004
package com.northbank.registration.auth.exception;

/**
 * Thrown by {@code PasswordResetService} when the supplied reset token is
 * absent, expired, or already used. Maps to HTTP 400 via
 * {@code GlobalExceptionHandler}.
 *
 * <p>A deliberately generic message is used to prevent token-oracle attacks:
 * callers cannot distinguish "not found" from "expired".</p>
 */
public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("Invalid or expired reset token");
    }
}
