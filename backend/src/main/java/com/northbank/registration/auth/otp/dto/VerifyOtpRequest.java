// Story: US-003
package com.northbank.registration.auth.otp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/verify-otp} (AC3).
 *
 * @param sessionToken the SESSION JWT issued by {@code POST /api/v1/auth/login}
 * @param otp          the 6-digit OTP the customer received by SMS
 */
public record VerifyOtpRequest(

        @Schema(
                description = "The SESSION JWT returned by POST /api/v1/auth/login",
                example     = "eyJhbGciOiJIUzI1NiJ9..."
        )
        @NotBlank(message = "sessionToken is required")
        String sessionToken,

        @Schema(
                description = "6-digit OTP received by SMS",
                example     = "123456"
        )
        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits")
        String otp

) {}
