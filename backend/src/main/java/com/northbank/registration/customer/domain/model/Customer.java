// Story: US-001 / US-002 / US-009
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
 * <p>US-002 adds: {@code failedLoginAttempts}, {@code lockedAt}, {@code passwordChangedAt}.</p>
 * <p>US-009 adds: {@code role} to support privileged endpoints.</p>
 *
 * @see CustomerStatus
 * @see CustomerRole
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

    /**
     * Application role. Defaults to {@code CUSTOMER} for all self-registered users.
     * Set to {@code ADMIN} by a privileged database operation or a future admin-management story.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private CustomerRole role = CustomerRole.CUSTOMER;

    // ── US-002: Login lockout fields ─────────────────────────────────────────

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    /**
     * Timestamp of the last password change.
     * Used by JwtAuthenticationFilter (US-004) to invalidate pre-change tokens.
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
