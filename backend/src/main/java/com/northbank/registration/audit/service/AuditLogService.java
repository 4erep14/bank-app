// Story: US-020
package com.northbank.registration.audit.service;

import com.northbank.registration.audit.domain.model.AuditActionType;
import com.northbank.registration.audit.domain.model.AuditLog;
import com.northbank.registration.audit.repository.AuditLogRepository;
import com.northbank.registration.audit.repository.AuditLogSpecifications;
import com.northbank.registration.audit.service.dto.AuditLogFilter;
import com.northbank.registration.audit.service.dto.AuditLogResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final String SYSTEM_ROLE = "SYSTEM";
    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditActionType actionType, String targetEntityType, UUID targetEntityId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID actorId = authentication != null && authentication.getPrincipal() instanceof UUID id ? id : null;
        String actorRole = authentication == null ? SYSTEM_ROLE : resolveRole(authentication);
        record(actionType, actorId, actorRole, targetEntityType, targetEntityId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            AuditActionType actionType,
            UUID actorId,
            String actorRole,
            String targetEntityType,
            UUID targetEntityId) {

        auditLogRepository.save(AuditLog.builder()
                .actorId(actorId)
                .actorRole(actorRole == null || actorRole.isBlank() ? SYSTEM_ROLE : actorRole)
                .actionType(actionType)
                .targetEntityType(targetEntityType)
                .targetEntityId(targetEntityId)
                .ipAddress(resolveIpAddress())
                .build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(AuditLogFilter filter, Pageable pageable) {
        return auditLogRepository.findAll(AuditLogSpecifications.matches(filter), pageable)
                .map(this::toResponse);
    }

    private String resolveRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse(SYSTEM_ROLE);
    }

    private String resolveIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getActorRole(),
                log.getActionType(),
                log.getTargetEntityType(),
                log.getTargetEntityId(),
                log.getTimestamp(),
                log.getIpAddress()
        );
    }
}
