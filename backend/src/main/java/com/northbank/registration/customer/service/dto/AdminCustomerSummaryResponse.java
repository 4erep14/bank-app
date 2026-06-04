// Story: US-019
package com.northbank.registration.customer.service.dto;

import com.northbank.registration.customer.domain.model.CustomerStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminCustomerSummaryResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        CustomerStatus status,
        OffsetDateTime createdAt
) {
}
