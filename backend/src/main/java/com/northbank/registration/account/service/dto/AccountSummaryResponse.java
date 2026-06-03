// Story: US-007
package com.northbank.registration.account.service.dto;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Represents one account item in the GET /api/v1/accounts list response (US-007 AC2).
 *
 * <p>Fields per acceptance criteria:
 * <ul>
 *   <li>AC2: accountNumber, type, balance, status</li>
 *   <li>AC5: balance is always serialized to exactly 2 decimal places</li>
 * </ul>
 * </p>
 */
@Schema(description = "Account summary item returned in the account list")
public record AccountSummaryResponse(

    @Schema(description = "10-digit account number", example = "4823901754")
    String accountNumber,

    @Schema(description = "Account type", example = "CHECKING")
    AccountType type,

    @Schema(description = "Current balance, always 2 decimal places (AC5)", example = "1250.00")
    BigDecimal balance,

    @Schema(description = "Account status", example = "ACTIVE")
    AccountStatus status
) {}
