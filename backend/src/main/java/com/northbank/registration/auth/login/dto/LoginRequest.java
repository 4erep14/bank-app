// Story: US-002
package com.northbank.registration.auth.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/v1/auth/login} (AC1).
 *
 * <p>The {@code password} field is write-only and must never appear in logs
 * or response bodies.</p>
 */
public record LoginRequest(

        @Schema(description = "Customer's registered email address", example = "ada.lovelace@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Schema(
                description = "Customer's password — write-only, never returned",
                example = "Str0ng!Pass",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Password is required")
        String password

) {}
