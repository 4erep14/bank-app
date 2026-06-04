// Story: US-017
package com.northbank.registration.fraud.service.dto;

import com.northbank.registration.fraud.domain.model.FraudAlertReviewStatus;
import com.northbank.registration.fraud.domain.model.FraudRuleConditionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

public record FraudAlertFilter(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime dateFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime dateTo,
        FraudAlertReviewStatus reviewStatus,
        FraudRuleConditionType ruleConditionType
) {
}
