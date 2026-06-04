// Story: US-014
package com.northbank.registration.fraud.service.dto;

import com.northbank.registration.fraud.domain.model.FraudRuleConditionType;
import com.northbank.registration.fraud.domain.model.FraudRuleStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FraudRuleResponse(
        UUID id,
        String name,
        FraudRuleConditionType conditionType,
        String thresholdValue,
        boolean active,
        FraudRuleStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
