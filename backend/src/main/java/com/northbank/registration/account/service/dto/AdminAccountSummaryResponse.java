// Story: US-009
package com.northbank.registration.account.service.dto;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Admin-facing account summary row (AC2).
 *
 * <p>Extends the customer-facing summary with owner PII:
 * {@code ownerFullName} and {@code ownerEmail}.</p>
 */
@Schema(description = "Admin view of a bank account with owner information")
public record AdminAccountSummaryResponse(

        @Schema(description = "Account UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "10-digit account number", example = "4823901754")
        String accountNumber,

        @Schema(description = "Account type", example = "CHECKING")
        AccountType type,

        @Schema(description = "Current balance", example = "1250.00")
        BigDecimal balance,

        @Schema(description = "Account status", example = "ACTIVE")
        AccountStatus status,

        @Schema(description = "Full name of the account owner", example = "Jane Doe")
        String ownerFullName,

        @Schema(description = "Email address of the account owner", example = "jane.doe@example.com")
        String ownerEmail
) {}
