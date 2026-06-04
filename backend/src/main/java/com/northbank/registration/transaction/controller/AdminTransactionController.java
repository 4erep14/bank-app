// Story: US-013
package com.northbank.registration.transaction.controller;

import com.northbank.registration.transaction.service.TransactionService;
import com.northbank.registration.transaction.service.dto.TransactionDetailResponse;
import com.northbank.registration.transaction.service.dto.TransactionFilter;
import com.northbank.registration.transaction.service.dto.TransactionSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@Tag(name = "Admin Transactions", description = "System-wide transaction visibility for bank administrators")
@SecurityRequirement(name = "bearerAuth")
public class AdminTransactionController {

    private final TransactionService transactionService;

    @Operation(
            summary = "List all transactions",
            description = "Returns a paged, filterable system-wide transaction list for users with the ADMIN role."
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<TransactionSummaryResponse>> listTransactions(
            TransactionFilter filter,
            @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(transactionService.listAdminTransactions(filter, pageable));
    }

    @Operation(
            summary = "Get transaction detail",
            description = "Returns transaction detail for users with the ADMIN role."
    )
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionDetailResponse> getTransaction(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getAdminTransaction(id));
    }
}
