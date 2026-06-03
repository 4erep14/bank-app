// Story: US-010
package com.northbank.registration.transaction.domain.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FraudEvaluationRequestedEvent(
        UUID transactionId,
        UUID customerId,
        BigDecimal amount,
        UUID sourceAccountId,
        UUID destinationAccountId,
        OffsetDateTime timestamp
) {
}
