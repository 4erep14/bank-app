// Story: US-014
package com.northbank.registration.fraud.exception;

public class InvalidFraudRuleThresholdException extends RuntimeException {
    public InvalidFraudRuleThresholdException(String message) {
        super(message);
    }
}
