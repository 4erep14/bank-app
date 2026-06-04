// Story: US-014
package com.northbank.registration.fraud.service.dto;

import com.northbank.registration.fraud.domain.model.FraudRuleConditionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFraudRuleRequest(
        @NotBlank
        @Size(max = 120)
        String name,

        @NotNull
        FraudRuleConditionType conditionType,

        @NotBlank
        @Size(max = 40)
        String thresholdValue,

        @NotNull
        Boolean active
) {
}
