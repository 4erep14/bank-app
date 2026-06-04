// Story: US-016
package com.northbank.registration.notification.service.dto;

import com.northbank.registration.notification.domain.model.NotificationStatus;
import com.northbank.registration.notification.domain.model.NotificationType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UUID transactionId,
        BigDecimal amount,
        OffsetDateTime timestamp,
        String triggeredRuleName,
        NotificationStatus status
) {
}
