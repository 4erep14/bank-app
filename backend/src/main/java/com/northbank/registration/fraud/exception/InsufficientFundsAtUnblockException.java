// Story: US-018
package com.northbank.registration.fraud.exception;

public class InsufficientFundsAtUnblockException extends RuntimeException {
    public InsufficientFundsAtUnblockException() {
        super("Insufficient funds at time of unblock");
    }
}
