// Story: US-014
package com.northbank.registration.fraud.service.dto;

import jakarta.validation.constraints.Size;

public record UpdateFraudRuleRequest(
        @Size(max = 120)
        String name,

        @Size(max = 40)
        String thresholdValue,

        Boolean active
) {
}
