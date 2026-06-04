// Story: US-011 | US-013
package com.northbank.registration.transaction.service.dto;

import com.northbank.registration.transaction.domain.model.TransactionStatus;
import com.northbank.registration.transaction.domain.model.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionSummaryResponse(
        UUID id,
        UUID customerId,
        TransactionType type,
        TransactionStatus status,
        BigDecimal amount,
        UUID sourceAccountId,
        UUID destinationAccountId,
        String description,
        OffsetDateTime timestamp
) {
}
