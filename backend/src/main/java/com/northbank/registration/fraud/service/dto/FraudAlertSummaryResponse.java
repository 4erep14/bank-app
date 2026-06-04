// Story: US-017
package com.northbank.registration.fraud.service.dto;

import com.northbank.registration.fraud.domain.model.FraudAlertReviewStatus;
import com.northbank.registration.fraud.domain.model.FraudRuleConditionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FraudAlertSummaryResponse(
        UUID alertId,
        UUID transactionId,
        BigDecimal amount,
        String customerFullName,
        String accountNumber,
        String triggeredRuleName,
        FraudRuleConditionType ruleConditionType,
        OffsetDateTime timestamp,
        FraudAlertReviewStatus reviewStatus
) {
}
