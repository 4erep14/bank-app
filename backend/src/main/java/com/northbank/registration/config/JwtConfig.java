// Story: US-002 / US-003 / US-005
package com.northbank.registration.config;

import com.northbank.registration.auth.otp.exception.InvalidSessionTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT configuration and token-generation/validation utility (ADR-002, ADR-003).
 *
 * <p>Bound to {@code security.jwt.*} properties.
 * The signing secret is read from the {@code SECURITY_JWT_SECRET} environment
 * variable — never hard-coded (must be ≥ 32 ASCII characters for HS256).</p>
 *
 * <h2>Token designs:</h2>
 * <ul>
 *   <li><b>SESSION</b> (ADR-002): {@code sub}=UUID, {@code type="SESSION"}, 5-min TTL.
 *       Stateless — used to correlate the login step with the OTP step.</li>
 *   <li><b>ACCESS</b> (ADR-003): {@code sub}=UUID, {@code type="ACCESS"}, 15-min TTL.
 *       Issued on successful OTP verification; authorises secured API calls.</li>
 * </ul>
 */
@Slf4j
@Component
@ConfigurationProperties(prefix = "security.jwt")
@Getter
@Setter
public class JwtConfig {

    /**
     * HMAC signing secret.  Must be supplied via {@code SECURITY_JWT_SECRET} env var.
     * Minimum 32 characters (256 bits) for HS256.
     */
    private String secret;

    /** Session token time-to-live in minutes. Default: 5 (ADR-002). */
    private int sessionTokenExpiryMinutes = 5;

    /** Access token time-to-live in minutes. Default: 15 (ADR-003). */
    private int accessTokenExpiryMinutes = 15;

    // ─────────────────────────────────────────────────────────────────────────
    // Token generation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates a short-lived SESSION JWT for the given customer (ADR-002).
     *
     * <p>The token is stateless — the caller must store it in memory and
     * present it to {@code POST /api/v1/auth/verify-otp} (US-003).</p>
     *
     * @param customerId the authenticated customer's UUID
     * @return compact, signed JWT string
     */
    public String generateSessionToken(UUID customerId) {
        SecretKey key = buildKey();
        Instant now   = Instant.now();
        return Jwts.builder()
                .subject(customerId.toString())
                .claim("type", "SESSION")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds((long) sessionTokenExpiryMinutes * 60)))
                .signWith(key)
                .compact();
    }

    /**
     * Generates a short-lived ACCESS JWT for the given customer (ADR-003).
     *
     * <p>Issued on successful OTP verification. Authorises calls to secured
     * endpoints (e.g. {@code GET /api/v1/profile} in US-005).</p>
     *
     * @param customerId the authenticated customer's UUID
     * @return compact, signed JWT string with {@code type="ACCESS"}
     */
    public String generateAccessToken(UUID customerId) {
        SecretKey key = buildKey();
        Instant now   = Instant.now();
        return Jwts.builder()
                .subject(customerId.toString())
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds((long) accessTokenExpiryMinutes * 60)))
                .signWith(key)
                .compact();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Token validation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates a SESSION JWT and extracts the customer UUID.
     *
     * <p>Verifies signature, expiry, and the {@code type="SESSION"} claim.
     * Throws {@link InvalidSessionTokenException} (→ HTTP 401) for any failure.</p>
     *
     * @param rawToken the compact JWT string to validate
     * @return the customer UUID extracted from the {@code sub} claim
     * @throws InvalidSessionTokenException if the token is invalid, expired, or not a SESSION token
     */
    public UUID validateSessionToken(String rawToken) {
        try {
            SecretKey key = buildKey();
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(rawToken)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"SESSION".equals(type)) {
                log.warn("JWT type mismatch — expected SESSION, got {}", type);
                throw new InvalidSessionTokenException();
            }

            return UUID.fromString(claims.getSubject());

        } catch (InvalidSessionTokenException e) {
            throw e;   // re-throw our own exception unmodified
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("SESSION JWT validation failed: {}", e.getMessage());
            throw new InvalidSessionTokenException();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACCESS token validation (US-005 / ADR-005)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates an ACCESS JWT and returns its full {@link Claims} payload.
     *
     * <p>Verifications performed (ADR-002/ADR-005):</p>
     * <ol>
     *   <li>HS256 signature against {@code security.jwt.secret}.</li>
     *   <li>Token is not expired ({@code exp > now()}).</li>
     *   <li>Claim {@code type == "ACCESS"}.</li>
     * </ol>
     *
     * <p>The returned {@link Claims} contains {@code sub} (customer UUID string)
     * and {@code iat} (issued-at epoch seconds), both consumed by
     * {@code JwtAuthenticationFilter} (US-005).</p>
     *
     * @param rawToken compact, signed JWT string
     * @return the verified {@link Claims} payload
     * @throws JwtException if the token is invalid, expired, or not an ACCESS token
     */
    public Claims validateAccessToken(String rawToken) {
        try {
            SecretKey key    = buildKey();
            Claims    claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(rawToken)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"ACCESS".equals(type)) {
                log.warn("JWT type mismatch — expected ACCESS, got {}", type);
                throw new JwtException("Not an ACCESS token");
            }

            return claims;

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("ACCESS JWT validation failed: {}", e.getMessage());
            throw new JwtException("Invalid or expired ACCESS token: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private SecretKey buildKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
