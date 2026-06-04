// Story: US-017
package com.northbank.registration.fraud.service.dto;

import com.northbank.registration.transaction.service.dto.TransactionDetailResponse;

import java.util.UUID;

public record FraudAlertDetailResponse(
        UUID alertId,
        FraudAlertSummaryResponse summary,
        String thresholdValue,
        String actualValue,
        UUID reviewedBy,
        String reviewedAt,
        TransactionDetailResponse transaction
) {
}
