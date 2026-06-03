// Story: US-002
package com.northbank.registration.auth.login.exception;

/**
 * Thrown by {@code AuthService} when a login attempt is made against a
 * {@code LOCKED} account, or when the 5th consecutive failed attempt
 * transitions the account to {@code LOCKED} (AC4, AC5).
 *
 * <p>Maps to HTTP 423 (Locked) with RFC 7807 ProblemDetail via
 * {@code GlobalExceptionHandler}.</p>
 */
public class AccountLockedException extends RuntimeException {

    public AccountLockedException() {
        super("Account locked due to too many failed login attempts");
    }
}
