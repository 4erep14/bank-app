// Story: US-006
package com.northbank.registration.account.service.dto;

import com.northbank.registration.account.domain.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for opening a new bank account (AC1).
 */
@Schema(description = "Request to open a new bank account")
public record OpenAccountRequest(

    @NotNull(message = "Account type is required")
    @Schema(
        description     = "Account type to open",
        example         = "CHECKING",
        allowableValues = {"CHECKING", "SAVINGS"}
    )
    AccountType type
) {}
