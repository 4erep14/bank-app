// Story: US-001 / US-002
package com.northbank.registration.customer.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity representing a registered banking customer.
 *
 * <p>password_hash is intentionally excluded from {@code toString()} and must never
 * appear in logs or API responses.</p>
 *
 * <p>US-002 adds: {@code failedLoginAttempts}, {@code lockedAt},
 * {@code passwordChangedAt} (forward-compat for US-004).</p>
 *
 * @see CustomerStatus
 */
@Entity
@Table(
    name = "customers",
    uniqueConstraints = @UniqueConstraint(name = "uq_customers_email", columnNames = "email")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "passwordHash")  // NEVER log the password hash
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * Stored lower-cased. Unique constraint enforced at both service and DB level
     * for race-safety (ADR-001).
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    /**
     * BCrypt hash (strength 12). Plaintext is NEVER stored or logged.
     */
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.PENDING_VERIFICATION;

    // ── US-002: Login lockout fields (V2 migration) ──────────────────────────

    /**
     * Consecutive failed login attempts since last success.
     * Reset to 0 on successful login. Reaches 5 → account is LOCKED (AC4).
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    /**
     * Timestamp when the account was locked due to too many failed attempts.
     * Null if the account has never been locked.
     */
    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    /**
     * Timestamp of the last password change.
     * Used by the JWT authentication filter (US-004) to invalidate
     * tokens issued before a password reset.
     * Null means password has never been explicitly changed.
     */
    @Column(name = "password_changed_at", columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime passwordChangedAt;

    // ── Audit timestamps ─────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
