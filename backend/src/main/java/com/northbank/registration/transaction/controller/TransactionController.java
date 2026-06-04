// Story: US-010
package com.northbank.registration.transaction.controller;

import com.northbank.registration.transaction.service.TransactionService;
import com.northbank.registration.transaction.service.dto.TransactionDetailResponse;
import com.northbank.registration.transaction.service.dto.TransactionFilter;
import com.northbank.registration.transaction.service.dto.TransactionSummaryResponse;
import com.northbank.registration.transaction.service.dto.TransferRequest;
import com.northbank.registration.transaction.service.dto.TransferResponse;
import com.northbank.registration.shared.security.AuthenticatedCustomer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
            summary = "List own transaction history",
            description = "Returns paged transactions for the authenticated customer with optional account, type, status, and date filters."
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<TransactionSummaryResponse>> listTransactions(
            TransactionFilter filter,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Object principal) {

        UUID customerId = AuthenticatedCustomer.resolveId(principal);
        return ResponseEntity.ok(transactionService.listCustomerTransactions(customerId, filter, pageable));
    }

    @Operation(
            summary = "Get own transaction detail",
            description = "Returns a transaction detail if it belongs to the authenticated customer."
    )
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransactionDetailResponse> getTransaction(
            @PathVariable UUID id,
            @AuthenticationPrincipal Object principal) {

        UUID customerId = AuthenticatedCustomer.resolveId(principal);
        return ResponseEntity.ok(transactionService.getCustomerTransaction(customerId, id));
    }

    @Operation(
            summary = "Transfer funds between own accounts",
            description = "Moves funds between two active accounts owned by the authenticated customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer accepted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or same-account transfer",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated request",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "403", description = "Account ownership violation",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Inactive account or insufficient funds",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/transfer", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal Object principal) {

        UUID customerId = AuthenticatedCustomer.resolveId(principal);
        log.debug("Transfer request: customerId={}, source={}, destination={}, amount={}",
                customerId, request.sourceAccountId(), request.destinationAccountId(), request.amount());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.transfer(customerId, request));
    }

}
