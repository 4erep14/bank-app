// Story: US-002
package com.northbank.registration.auth.login.exception;

/**
 * Thrown by {@code AuthService} when the supplied email is not found or
 * the password does not match the stored BCrypt hash (AC3).
 *
 * <p>Maps to HTTP 401 with RFC 7807 ProblemDetail via
 * {@code GlobalExceptionHandler}. The message is intentionally generic —
 * "Invalid email or password" — to prevent user-enumeration attacks.</p>
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
