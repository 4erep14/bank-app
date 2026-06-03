// Story: US-003
package com.northbank.registration.auth.otp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body for a successful {@code POST /api/v1/auth/verify-otp} (AC3).
 *
 * @param accessToken  short-lived JWT (15 min, {@code type="ACCESS"}) for calling secured APIs
 * @param refreshToken opaque 43-char Base64URL token (7-day TTL) for obtaining new access tokens
 */
public record OtpVerifyResponse(

        @Schema(
                description = "Short-lived access JWT (15-minute expiry, type=ACCESS). " +
                              "Present as Bearer token on secured endpoints.",
                example     = "eyJhbGciOiJIUzI1NiJ9..."
        )
        String accessToken,

        @Schema(
                description = "Opaque 7-day refresh token. Store securely; use to obtain new access tokens.",
                example     = "dGhpcyBpcyBhIHNhbXBsZSByZWZyZXNoIHRva2Vu"
        )
        String refreshToken

) {}
