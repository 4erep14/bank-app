// Story: US-010
package com.northbank.registration.transaction.service;

import com.northbank.registration.fraud.domain.model.FraudRuleConditionType;

public record FraudEvaluationResult(
        boolean blocked,
        String ruleName,
        FraudRuleConditionType ruleConditionType,
        String thresholdValue,
        String actualValue
) {
    public static FraudEvaluationResult allowed() {
        return new FraudEvaluationResult(false, null, null, null, null);
    }

    public static FraudEvaluationResult blockedResult() {
        return new FraudEvaluationResult(true, null, null, null, null);
    }

    public static FraudEvaluationResult blockedResult(
            String ruleName,
            FraudRuleConditionType ruleConditionType,
            String thresholdValue,
            String actualValue) {

        return new FraudEvaluationResult(true, ruleName, ruleConditionType, thresholdValue, actualValue);
    }
}
