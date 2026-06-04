// Story: US-012 | US-013
package com.northbank.registration.transaction.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException() {
        super("Transaction not found");
    }
}
