// Story: US-006
package com.northbank.registration.account.exception;

import com.northbank.registration.account.domain.model.AccountType;

/**
 * Thrown when a customer tries to open a second account of the same type (AC5).
 * Maps to HTTP 409 via the global exception handler.
 *
 * <p>Error message is fixed to match the spec: "Account of this type already exists"</p>
 */
public class DuplicateAccountTypeException extends RuntimeException {

    private final AccountType type;

    public DuplicateAccountTypeException(AccountType type) {
        super("Account of this type already exists");
        this.type = type;
    }

    public AccountType getType() {
        return type;
    }
}
