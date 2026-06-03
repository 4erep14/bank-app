// Story: US-001
package com.northbank.registration.customer.exception;

/**
 * Thrown by {@code CustomerService} when a registration attempt uses an email
 * address that already exists in the {@code customers} table.
 *
 * <p>Mapped to HTTP 409 Conflict by {@code GlobalExceptionHandler}.</p>
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException(String email) {
        super("Email already registered: " + email);
    }
}
