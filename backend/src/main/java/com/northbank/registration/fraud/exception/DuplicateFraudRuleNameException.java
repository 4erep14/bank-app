// Story: US-014
package com.northbank.registration.fraud.exception;

public class DuplicateFraudRuleNameException extends RuntimeException {
    public DuplicateFraudRuleNameException() {
        super("Rule name already exists");
    }
}
