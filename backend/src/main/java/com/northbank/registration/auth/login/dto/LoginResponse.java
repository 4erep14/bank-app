// Story: US-002
package com.northbank.registration.auth.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body for a successful {@code POST /api/v1/auth/login} (AC2).
 *
 * <p>A {@code status} of {@code "2FA_REQUIRED"} signals the client to proceed
 * to the OTP verification step (US-003). The {@code sessionToken} is a
 * short-lived (5-min) stateless JWT carrying {@code sub = customerId} and
 * {@code type = "SESSION"} — see ADR-002 sessionToken design.</p>
 */
public record LoginResponse(

        @Schema(description = "Always '2FA_REQUIRED' — instructs the client to proceed to OTP verification",
                example = "2FA_REQUIRED")
        String status,

        @Schema(description = "Short-lived signed JWT (5-min expiry). Present this to POST /api/v1/auth/verify-otp.",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String sessionToken

) {}
