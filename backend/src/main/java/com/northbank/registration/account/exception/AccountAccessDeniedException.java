// Story: US-008
package com.northbank.registration.account.exception;

/**
 * Thrown when a customer attempts to access an account that belongs
 * to a different customer (AC3).
 * Maps to HTTP 403 via GlobalExceptionHandler.
 */
public class AccountAccessDeniedException extends RuntimeException {

    public AccountAccessDeniedException() {
        super("Access to this account is denied");
    }
}
