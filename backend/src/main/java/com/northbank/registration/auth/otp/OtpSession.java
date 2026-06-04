// Story: US-003
package com.northbank.registration.auth.otp;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity representing a pending OTP verification session (ADR-003).
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>Created by {@code OtpService.createOtpSession()} immediately after a
 *       successful password check in {@code AuthService.login()} (US-002 step 1).</li>
 *   <li>Consumed (marked {@code invalidated=true}) by {@code OtpService.verifyOtp()}
 *       on correct OTP submission — prevents replay attacks.</li>
 *   <li>Also invalidated after 3 consecutive wrong OTP attempts (AC5).</li>
 * </ol>
 *
 * <p>{@code sessionTokenHash}: SHA-256 hex of the raw SESSION JWT issued by
 * ADR-002. Never the raw JWT string — hash-at-rest mirrors the ADR-004 pattern.</p>
 *
 * <p>{@code createdAt} is intentionally mutable (no {@code @CreationTimestamp})
 * so that {@code resendOtp} can reset it for rate-limit tracking.</p>
 */
@Entity
@Table(
    name = "otp_sessions",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_otp_sessions_token_hash",
        columnNames = "session_token_hash"
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "otpCode")   // never log the OTP code
public class OtpSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** FK to {@code customers.id} — stored as plain UUID, no lazy-load join needed. */
    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    /** SHA-256 hex digest of the raw SESSION JWT — 64 hex chars. */
    @Column(name = "session_token_hash", nullable = false, length = 64)
    private String sessionTokenHash;

    /** 6-digit zero-padded OTP code. Never returned in any API response. */
    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    /** When this OTP expires. Default: 5 minutes from creation. */
    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime expiresAt;

    /**
     * Number of consecutive wrong OTP submissions.
     * When this reaches 3 the session is invalidated (AC5).
     */
    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private int failedAttempts = 0;

    /**
     * {@code true} once the session has been consumed (valid OTP) or
     * invalidated by 3 consecutive failures. Prevents replay.
     */
    @Column(name = "invalidated", nullable = false)
    @Builder.Default
    private boolean invalidated = false;

    /**
     * Mutable creation timestamp — reset by {@code resendOtp} for the
     * 60-second rate-limit window.
     */
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
