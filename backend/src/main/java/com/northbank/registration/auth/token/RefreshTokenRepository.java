// Story: US-003
package com.northbank.registration.auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link RefreshToken} (ADR-003).
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Looks up a refresh token by its SHA-256 hex hash.
     *
     * @param tokenHash 64-character hex digest of the raw opaque token
     * @return the matching token, or empty if none exists
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
