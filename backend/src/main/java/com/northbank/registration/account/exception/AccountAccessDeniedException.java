// Story: US-008
package com.northbank.registration.account.exception;

/**
 * Thrown when a customer tries to access another customer's account.
 * Maps to HTTP 403 via the global exception handler.
 */
public class AccountAccessDeniedException extends RuntimeException {

    public AccountAccessDeniedException() {
        super("Access to this account is denied");
    }
}
