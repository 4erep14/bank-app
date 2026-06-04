// Story: US-020
package com.northbank.registration.audit.service.dto;

import com.northbank.registration.audit.domain.model.AuditActionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogFilter(
        UUID actorId,
        AuditActionType actionType,
        OffsetDateTime dateFrom,
        OffsetDateTime dateTo
) {
}
