// Story: US-018
package com.northbank.registration.fraud.exception;

public class TransactionAlreadyResolvedException extends RuntimeException {
    public TransactionAlreadyResolvedException() {
        super("Transaction is already resolved");
    }
}
