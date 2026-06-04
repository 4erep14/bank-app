// Story: US-020
package com.northbank.registration.audit.controller;

import com.northbank.registration.audit.domain.model.AuditActionType;
import com.northbank.registration.audit.service.AuditLogService;
import com.northbank.registration.audit.service.dto.AuditLogFilter;
import com.northbank.registration.audit.service.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogController {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) AuditActionType actionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        int resolvedSize = size > 0 ? size : DEFAULT_PAGE_SIZE;
        return ResponseEntity.ok(auditLogService.list(
                new AuditLogFilter(actorId, actionType, dateFrom, dateTo),
                PageRequest.of(page, resolvedSize, Sort.by(Sort.Direction.DESC, "timestamp"))
        ));
    }

    @PutMapping
    public ResponseEntity<Void> putNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @PatchMapping
    public ResponseEntity<Void> patchNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}
