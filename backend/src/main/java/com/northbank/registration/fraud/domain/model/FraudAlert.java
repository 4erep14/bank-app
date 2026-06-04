// Story: US-015 / US-017 / US-018
package com.northbank.registration.fraud.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_alerts", indexes = {
        @Index(name = "idx_fraud_alerts_timestamp", columnList = "timestamp"),
        @Index(name = "idx_fraud_alerts_review_status", columnList = "review_status"),
        @Index(name = "idx_fraud_alerts_rule_condition_type", columnList = "rule_condition_type"),
        @Index(name = "idx_fraud_alerts_transaction", columnList = "transaction_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "rule_name", nullable = false, length = 120, updatable = false)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_condition_type", nullable = false, length = 40, updatable = false)
    private FraudRuleConditionType ruleConditionType;

    @Column(name = "threshold_value", nullable = false, length = 40, updatable = false)
    private String thresholdValue;

    @Column(name = "actual_value", nullable = false, length = 80, updatable = false)
    private String actualValue;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private OffsetDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    @Builder.Default
    private FraudAlertReviewStatus reviewStatus = FraudAlertReviewStatus.PENDING_REVIEW;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
