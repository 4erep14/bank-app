// Story: US-010
package com.northbank.registration.transaction.exception;

public class InactiveAccountException extends RuntimeException {
    public InactiveAccountException() {
        super("Account is inactive");
    }
}
