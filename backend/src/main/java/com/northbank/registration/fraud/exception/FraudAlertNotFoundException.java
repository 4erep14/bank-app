// Story: US-017 / US-018
package com.northbank.registration.fraud.exception;

public class FraudAlertNotFoundException extends RuntimeException {
    public FraudAlertNotFoundException() {
        super("Fraud alert not found");
    }
}
