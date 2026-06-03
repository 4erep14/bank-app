// Story: US-005
package com.northbank.registration.profile;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * Response DTO for {@code GET /api/v1/profile} and {@code PATCH /api/v1/profile}.
 *
 * <p>Returns the authenticated customer's profile fields (AC1).
 * {@code email} and {@code dateOfBirth} are included but are read-only — they
 * cannot be changed via the PATCH endpoint (AC3).</p>
 */
@Schema(description = "Customer profile details")
public record ProfileResponse(

        @Schema(description = "Customer's first name", example = "Ada")
        String firstName,

        @Schema(description = "Customer's last name", example = "Lovelace")
        String lastName,

        @Schema(description = "Customer's email address (read-only)", example = "ada.lovelace@example.com")
        String email,

        @Schema(description = "Phone number in E.164 format", example = "+14155552671")
        String phoneNumber,

        @Schema(description = "Date of birth (yyyy-MM-dd) — read-only", example = "1990-12-10")
        LocalDate dateOfBirth

) {}
