// Story: US-008
package com.northbank.registration.account.service.dto;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Full account detail payload returned by GET /api/v1/accounts/{id} (AC2).
 *
 * <p>Fields: id, accountNumber, type, balance, status, createdAt.</p>
 */
@Schema(description = "Full details of a single bank account")
public record AccountDetailResponse(

        @Schema(description = "Unique account ID (UUID)",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Unique 10-digit account number", example = "4823901754")
        String accountNumber,

        @Schema(description = "Account type", example = "CHECKING",
                allowableValues = {"CHECKING", "SAVINGS"})
        AccountType type,

        @Schema(description = "Current balance with two decimal places", example = "1250.00")
        BigDecimal balance,

        @Schema(description = "Account status", example = "ACTIVE",
                allowableValues = {"ACTIVE", "CLOSED", "FROZEN"})
        AccountStatus status,

        @Schema(description = "ISO-8601 timestamp when the account was created")
        OffsetDateTime createdAt
) {}
