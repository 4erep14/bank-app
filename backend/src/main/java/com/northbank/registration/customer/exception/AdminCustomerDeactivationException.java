// Story: US-019
package com.northbank.registration.customer.exception;

public class AdminCustomerDeactivationException extends RuntimeException {

    public AdminCustomerDeactivationException() {
        super("Cannot deactivate an admin account");
    }
}
