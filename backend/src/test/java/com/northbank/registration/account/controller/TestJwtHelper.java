// Story: US-008
package com.northbank.registration.account.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public final class TestJwtHelper {

    private static final String SECRET = "northbank-test-jwt-secret-us002!!!";

    private TestJwtHelper() {
    }

    public static String generateToken(UUID customerId) {
        return generateToken(customerId, null);
    }

    public static String generateAdminToken(UUID customerId) {
        return generateToken(customerId, "ADMIN");
    }

    public static String generateFraudAnalystToken(UUID customerId) {
        return generateToken(customerId, "FRAUD_ANALYST");
    }

    private static String generateToken(UUID customerId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(customerId.toString())
                .claim("type", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(key);
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
    }
}
