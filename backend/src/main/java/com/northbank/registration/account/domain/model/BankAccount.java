// Story: US-006
package com.northbank.registration.account.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity representing a bank account (Checking or Savings).
 *
 * <p>Business rules enforced here:
 * <ul>
 *   <li>A customer may hold at most one CHECKING and one SAVINGS account
 *       — enforced by the unique constraint {@code uq_accounts_customer_type}.</li>
 *   <li>Account opens with {@code balance = 0.00} and {@code status = ACTIVE}.</li>
 *   <li>Account number is a unique 10-digit string generated at service layer.</li>
 * </ul>
 * </p>
 *
 * @see AccountType
 * @see AccountStatus
 */
@Entity
@Table(
    name = "accounts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_accounts_account_number", columnNames = "account_number"),
        @UniqueConstraint(name = "uq_accounts_customer_type",  columnNames = {"customer_id", "type"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Unique 10-digit numeric string generated at service layer (AC2).
     * Immutable after creation.
     */
    @Column(name = "account_number", nullable = false, length = 10, updatable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private AccountType type;

    /**
     * Always starts at 0.00 on account creation (AC3).
     */
    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Starts ACTIVE on creation (AC3).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    /** FK to the owning customer. Never null, immutable after creation. */
    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
