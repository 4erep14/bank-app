// Story: US-006
package com.northbank.registration.account.service.dto;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response payload returned on successful account creation (AC4).
 * Returns: id, accountNumber, type, balance, status, createdAt.
 */
@Schema(description = "Newly opened bank account details")
public record OpenAccountResponse(

    @Schema(description = "Unique account ID (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID id,

    @Schema(description = "Unique 10-digit account number", example = "4823901754")
    String accountNumber,

    @Schema(description = "Account type", example = "CHECKING")
    AccountType type,

    @Schema(description = "Current balance (always 0.00 on creation)", example = "0.00")
    BigDecimal balance,

    @Schema(description = "Account status", example = "ACTIVE")
    AccountStatus status,

    @Schema(description = "Account creation timestamp")
    OffsetDateTime createdAt
) {}
