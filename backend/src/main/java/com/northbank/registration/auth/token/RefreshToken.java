// Story: US-003
package com.northbank.registration.auth.token;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity representing a long-lived opaque refresh token (ADR-003).
 *
 * <p>Issued by {@code OtpService.verifyOtp()} on successful 2FA completion.
 * Only the SHA-256 hex hash of the raw token value is stored — the raw token
 * is returned to the client once and never persisted.</p>
 *
 * <p>Default TTL: 7 days. Revocation flag allows explicit logout (future story).</p>
 */
@Entity
@Table(
    name = "refresh_tokens",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_refresh_tokens_token_hash",
        columnNames = "token_hash"
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** FK to {@code customers.id} — plain UUID, no join required at this layer. */
    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    /** SHA-256 hex digest of the raw opaque refresh token — 64 hex chars. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /** When this refresh token expires (default: 7 days from issue). */
    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime expiresAt;

    /** Set to {@code true} when the token is explicitly revoked (e.g. logout). */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
