// Story: US-001
package com.northbank.registration.customer.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.northbank.registration.validation.Password;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Request DTO for {@code POST /api/v1/customers} — Customer Self-Registration.
 *
 * <p>The {@code password} field is write-only: it is accepted on input,
 * hashed immediately in the service, and NEVER returned in any response
 * or written to logs.</p>
 */
@Builder
public record RegisterCustomerRequest(

        @Schema(description = "Customer's first name", example = "Ada")
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Schema(description = "Customer's last name", example = "Lovelace")
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Schema(description = "Customer's email address — must be unique", example = "ada.lovelace@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Schema(description = "Phone number in E.164 format", example = "+14155552671")
        @NotBlank(message = "Phone number is required")
        @Pattern(
                regexp = "^\\+[1-9]\\d{1,14}$",
                message = "Phone number must be in E.164 format (e.g. +14155552671)"
        )
        String phoneNumber,

        @Schema(description = "Date of birth (ISO-8601 yyyy-MM-dd)", example = "1990-12-10")
        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dateOfBirth,

        @Schema(
                description = "Password — write-only, never returned. " +
                              "Must be ≥8 characters with at least one uppercase, " +
                              "one lowercase, one digit, and one special character.",
                example = "Str0ng!Pass",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Password is required")
        @Password
        String password

) {}
