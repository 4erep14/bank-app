// Story: US-015 / US-016
package com.northbank.registration.transaction.service;

import com.northbank.registration.fraud.domain.model.FraudAlert;
import com.northbank.registration.fraud.domain.model.FraudAlertReviewStatus;
import com.northbank.registration.fraud.domain.model.FraudRule;
import com.northbank.registration.fraud.domain.model.FraudRuleConditionType;
import com.northbank.registration.fraud.domain.model.FraudRuleStatus;
import com.northbank.registration.fraud.repository.FraudAlertRepository;
import com.northbank.registration.fraud.repository.FraudRuleRepository;
import com.northbank.registration.notification.service.NotificationService;
import com.northbank.registration.transaction.domain.event.FraudEvaluationRequestedEvent;
import com.northbank.registration.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Synchronous rule-based fraud evaluation for transfers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AllowAllFraudEvaluationService implements FraudEvaluationPort {

    private final FraudRuleRepository fraudRuleRepository;
    private final FraudAlertRepository fraudAlertRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    @Override
    public FraudEvaluationResult evaluate(FraudEvaluationRequestedEvent event) {
        List<FraudRule> activeRules = fraudRuleRepository.findAllByActiveTrueAndStatus(FraudRuleStatus.ACTIVE)
                .stream()
                .sorted(Comparator.comparing(FraudRule::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        for (FraudRule rule : activeRules) {
            FraudEvaluationResult result = evaluateRule(rule, event);
            if (result.blocked()) {
                persistAlertAndNotification(event, result);
                return result;
            }
        }

        return FraudEvaluationResult.allowed();
    }

    private FraudEvaluationResult evaluateRule(FraudRule rule, FraudEvaluationRequestedEvent event) {
        return switch (rule.getConditionType()) {
            case AMOUNT_EXCEEDS -> evaluateAmount(rule, event);
            case TRANSACTION_FREQUENCY -> evaluateFrequency(rule, event);
            case UNUSUAL_HOUR -> evaluateUnusualHour(rule, event);
        };
    }

    private FraudEvaluationResult evaluateAmount(FraudRule rule, FraudEvaluationRequestedEvent event) {
        BigDecimal threshold = new BigDecimal(rule.getThresholdValue());
        if (event.amount().compareTo(threshold) > 0) {
            return blocked(rule, event.amount().toPlainString());
        }
        return FraudEvaluationResult.allowed();
    }

    private FraudEvaluationResult evaluateFrequency(FraudRule rule, FraudEvaluationRequestedEvent event) {
        int threshold = Integer.parseInt(rule.getThresholdValue());
        long count = transactionRepository.countByCustomerIdAndTimestampAfter(
                event.customerId(),
                event.timestamp().minus(Duration.ofSeconds(60))
        );
        if (count > threshold) {
            return blocked(rule, Long.toString(count));
        }
        return FraudEvaluationResult.allowed();
    }

    private FraudEvaluationResult evaluateUnusualHour(FraudRule rule, FraudEvaluationRequestedEvent event) {
        HourRange range = parseHourRange(rule.getThresholdValue());
        LocalTime actual = event.timestamp().toLocalTime();
        if (isInRange(actual, range.start(), range.end())) {
            return blocked(rule, actual.toString());
        }
        return FraudEvaluationResult.allowed();
    }

    private void persistAlertAndNotification(FraudEvaluationRequestedEvent event, FraudEvaluationResult result) {
        fraudAlertRepository.save(FraudAlert.builder()
                .transactionId(event.transactionId())
                .ruleName(result.ruleName())
                .ruleConditionType(result.ruleConditionType())
                .thresholdValue(result.thresholdValue())
                .actualValue(result.actualValue())
                .timestamp(OffsetDateTime.now())
                .reviewStatus(FraudAlertReviewStatus.PENDING_REVIEW)
                .build());

        notificationService.sendTransactionBlocked(
                event.customerId(),
                event.transactionId(),
                event.amount(),
                event.timestamp(),
                result.ruleName()
        );

        log.info("Fraud alert created: transactionId={}, rule={}", event.transactionId(), result.ruleName());
    }

    private FraudEvaluationResult blocked(FraudRule rule, String actualValue) {
        return FraudEvaluationResult.blockedResult(
                rule.getName(),
                rule.getConditionType(),
                rule.getThresholdValue(),
                actualValue
        );
    }

    private HourRange parseHourRange(String thresholdValue) {
        String[] parts = thresholdValue.split("-");
        return new HourRange(LocalTime.parse(parts[0].trim()), LocalTime.parse(parts[1].trim()));
    }

    private boolean isInRange(LocalTime actual, LocalTime start, LocalTime end) {
        if (start.isBefore(end)) {
            return !actual.isBefore(start) && actual.isBefore(end);
        }
        return !actual.isBefore(start) || actual.isBefore(end);
    }

    private record HourRange(LocalTime start, LocalTime end) {
    }
}
