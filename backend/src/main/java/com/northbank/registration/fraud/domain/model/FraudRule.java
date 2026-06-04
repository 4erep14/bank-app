// Story: US-014
package com.northbank.registration.fraud.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "fraud_rules",
        uniqueConstraints = @UniqueConstraint(name = "uq_fraud_rules_name", columnNames = "name"),
        indexes = @Index(name = "idx_fraud_rules_active_status", columnList = "active,status")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 40, updatable = false)
    private FraudRuleConditionType conditionType;

    @Column(name = "threshold_value", nullable = false, length = 40)
    private String thresholdValue;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FraudRuleStatus status = FraudRuleStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
