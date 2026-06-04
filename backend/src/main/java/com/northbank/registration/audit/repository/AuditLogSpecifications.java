// Story: US-020
package com.northbank.registration.audit.repository;

import com.northbank.registration.audit.domain.model.AuditLog;
import com.northbank.registration.audit.service.dto.AuditLogFilter;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> matches(AuditLogFilter filter) {
        Specification<AuditLog> spec = Specification.where(null);
        if (filter.actorId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorId"), filter.actorId()));
        }
        if (filter.actionType() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actionType"), filter.actionType()));
        }
        if (filter.dateFrom() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), filter.dateFrom()));
        }
        if (filter.dateTo() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), filter.dateTo()));
        }
        return spec;
    }
}
