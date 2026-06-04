// Story: US-016
package com.northbank.registration.notification.service;

import com.northbank.registration.notification.domain.model.Notification;
import com.northbank.registration.notification.domain.model.NotificationStatus;
import com.northbank.registration.notification.domain.model.NotificationType;
import com.northbank.registration.notification.repository.NotificationRepository;
import com.northbank.registration.notification.service.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse sendTransactionBlocked(
            UUID customerId,
            UUID transactionId,
            BigDecimal amount,
            OffsetDateTime timestamp,
            String triggeredRuleName) {

        Notification notification = notificationRepository.save(Notification.builder()
                .customerId(customerId)
                .type(NotificationType.TRANSACTION_BLOCKED)
                .transactionId(transactionId)
                .amount(amount)
                .timestamp(timestamp)
                .triggeredRuleName(triggeredRuleName)
                .status(NotificationStatus.SENT)
                .build());
        log.info("Transaction blocked notification persisted: customerId={}, transactionId={}, rule={}",
                customerId, transactionId, triggeredRuleName);
        return toResponse(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> listCustomerNotifications(UUID customerId, Pageable pageable) {
        return notificationRepository.findAllByCustomerId(customerId, pageable)
                .map(this::toResponse);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTransactionId(),
                notification.getAmount(),
                notification.getTimestamp(),
                notification.getTriggeredRuleName(),
                notification.getStatus()
        );
    }
}
