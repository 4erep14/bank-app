// Story: US-008
package com.northbank.registration.account.exception;

import java.util.UUID;

/**
 * Thrown when no account exists for the given ID.
 * Maps to HTTP 404 via GlobalExceptionHandler.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID id) {
        super("Account not found");
    }
}
