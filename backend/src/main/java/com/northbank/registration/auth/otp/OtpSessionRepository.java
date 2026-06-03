// Story: US-003
package com.northbank.registration.auth.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link OtpSession} (ADR-003).
 */
@Repository
public interface OtpSessionRepository extends JpaRepository<OtpSession, UUID> {

    /**
     * Looks up the OTP session by the SHA-256 hex hash of the raw SESSION JWT.
     *
     * @param sessionTokenHash 64-character hex digest
     * @return the matching session, or empty if none exists
     */
    Optional<OtpSession> findBySessionTokenHash(String sessionTokenHash);
}
