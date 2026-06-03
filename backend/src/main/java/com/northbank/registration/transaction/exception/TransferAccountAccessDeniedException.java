// Story: US-010
package com.northbank.registration.transaction.exception;

public class TransferAccountAccessDeniedException extends RuntimeException {
    public TransferAccountAccessDeniedException() {
        super("Access denied");
    }
}
