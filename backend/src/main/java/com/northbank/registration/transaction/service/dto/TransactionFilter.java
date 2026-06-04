// Story: US-011 | US-013
package com.northbank.registration.transaction.service.dto;

import com.northbank.registration.transaction.domain.model.TransactionStatus;
import com.northbank.registration.transaction.domain.model.TransactionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionFilter(
        UUID accountId,
        UUID customerId,
        TransactionType type,
        TransactionStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        OffsetDateTime to
) {
}
