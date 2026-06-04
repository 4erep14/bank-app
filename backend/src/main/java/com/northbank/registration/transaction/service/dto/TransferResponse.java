// Story: US-010
package com.northbank.registration.transaction.service.dto;

import com.northbank.registration.transaction.domain.model.TransactionStatus;

import java.util.UUID;

public record TransferResponse(
        UUID transactionId,
        TransactionStatus status
) {
}
