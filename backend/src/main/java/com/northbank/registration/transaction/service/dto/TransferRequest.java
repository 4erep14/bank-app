// Story: US-010
package com.northbank.registration.transaction.service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull(message = "Source account is required")
        UUID sourceAccountId,

        @NotNull(message = "Destination account is required")
        UUID destinationAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be greater than 0.00")
        @DecimalMax(value = "10000.00", message = "Amount must be at most 10000.00")
        @Digits(integer = 17, fraction = 2, message = "Amount must use dollars and cents only")
        BigDecimal amount,

        @Size(max = 255, message = "Description must be 255 characters or fewer")
        String description
) {
}
