// Story: US-014
package com.northbank.registration.fraud.exception;

public class FraudRuleNotFoundException extends RuntimeException {
    public FraudRuleNotFoundException() {
        super("Fraud rule not found");
    }
}
