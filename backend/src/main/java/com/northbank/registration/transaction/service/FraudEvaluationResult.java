// Story: US-010
package com.northbank.registration.transaction.service;

public record FraudEvaluationResult(boolean blocked) {
    public static FraudEvaluationResult allowed() {
        return new FraudEvaluationResult(false);
    }

    public static FraudEvaluationResult blockedResult() {
        return new FraudEvaluationResult(true);
    }
}
