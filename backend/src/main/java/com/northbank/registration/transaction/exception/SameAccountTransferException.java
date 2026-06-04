// Story: US-010
package com.northbank.registration.transaction.exception;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException() {
        super("Source and destination accounts must differ");
    }
}
