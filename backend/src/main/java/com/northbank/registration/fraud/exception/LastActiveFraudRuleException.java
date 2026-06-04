// Story: US-014
package com.northbank.registration.fraud.exception;

public class LastActiveFraudRuleException extends RuntimeException {
    public LastActiveFraudRuleException() {
        super("At least one active rule must remain");
    }
}
