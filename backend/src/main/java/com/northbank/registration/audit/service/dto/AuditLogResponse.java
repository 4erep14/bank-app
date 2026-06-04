// Story: US-020
package com.northbank.registration.audit.service.dto;

import com.northbank.registration.audit.domain.model.AuditActionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorRole,
        AuditActionType actionType,
        String targetEntityType,
        UUID targetEntityId,
        OffsetDateTime timestamp,
        String ipAddress
) {
}
