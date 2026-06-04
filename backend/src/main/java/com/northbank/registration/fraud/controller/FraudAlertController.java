// Story: US-017 / US-018
package com.northbank.registration.fraud.controller;

import com.northbank.registration.fraud.service.FraudAlertService;
import com.northbank.registration.fraud.service.dto.FraudAlertDetailResponse;
import com.northbank.registration.fraud.service.dto.FraudAlertFilter;
import com.northbank.registration.fraud.service.dto.FraudAlertSummaryResponse;
import com.northbank.registration.shared.security.AuthenticatedCustomer;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud/alerts")
@RequiredArgsConstructor
@Tag(name = "Fraud Alerts", description = "Fraud analyst alert review workflow")
@SecurityRequirement(name = "bearerAuth")
public class FraudAlertController {

    private final FraudAlertService fraudAlertService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<FraudAlertSummaryResponse>> list(
            FraudAlertFilter filter,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(fraudAlertService.list(filter, pageable));
    }

    @GetMapping(value = "/{alertId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FraudAlertDetailResponse> get(@PathVariable UUID alertId) {
        return ResponseEntity.ok(fraudAlertService.get(alertId));
    }

    @PostMapping(value = "/{alertId}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FraudAlertDetailResponse> approve(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal Object principal) {

        return ResponseEntity.ok(fraudAlertService.approve(alertId, AuthenticatedCustomer.resolveId(principal)));
    }

    @PostMapping(value = "/{alertId}/reject", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FraudAlertDetailResponse> reject(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal Object principal) {

        return ResponseEntity.ok(fraudAlertService.reject(alertId, AuthenticatedCustomer.resolveId(principal)));
    }
}
