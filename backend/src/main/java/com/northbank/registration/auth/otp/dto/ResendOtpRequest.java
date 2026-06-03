// Story: US-003
package com.northbank.registration.auth.otp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/auth/resend-otp}.
 *
 * @param sessionToken the SESSION JWT issued by {@code POST /api/v1/auth/login}
 */
public record ResendOtpRequest(

        @Schema(
                description = "The SESSION JWT returned by POST /api/v1/auth/login",
                example     = "eyJhbGciOiJIUzI1NiJ9..."
        )
        @NotBlank(message = "sessionToken is required")
        String sessionToken

) {}
