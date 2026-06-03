// Story: US-001
package com.northbank.registration.customer.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response DTO for a successful customer registration (HTTP 201).
 * Returns only the newly created customer {@code id} — AC6.
 * No PII is included beyond the opaque UUID.
 */
public record RegisterCustomerResponse(

        @Schema(description = "Newly created customer identifier", example = "9f1c2e7a-3b4d-4c5e-8a9b-0c1d2e3f4a5b")
        UUID id

) {}
