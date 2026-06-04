// Story: US-019
package com.northbank.registration.customer.controller;

import com.northbank.registration.customer.domain.model.CustomerStatus;
import com.northbank.registration.customer.service.AdminCustomerService;
import com.northbank.registration.customer.service.dto.AdminCustomerSummaryResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    @GetMapping
    public ResponseEntity<Page<AdminCustomerSummaryResponse>> listCustomers(
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminCustomerService.listCustomers(status, page, size));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<AdminCustomerSummaryResponse> deactivateCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(adminCustomerService.deactivateCustomer(id));
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<AdminCustomerSummaryResponse> unlockCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(adminCustomerService.unlockCustomer(id));
    }
}
