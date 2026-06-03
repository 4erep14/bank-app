// Story: US-003
package com.northbank.registration.auth.otp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response body for a successful {@code POST /api/v1/auth/resend-otp}.
 *
 * @param message human-readable confirmation that a new OTP was sent
 */
public record ResendOtpResponse(

        @Schema(description = "Confirmation message", example = "OTP sent")
        String message

) {}
