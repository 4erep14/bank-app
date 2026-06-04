// Story: US-006 + US-007 + US-008
package com.northbank.registration.account.controller;

import com.northbank.registration.account.service.AccountService;
import com.northbank.registration.account.service.dto.AccountDetailResponse;
import com.northbank.registration.account.service.dto.AccountSummaryResponse;
import com.northbank.registration.account.service.dto.OpenAccountRequest;
import com.northbank.registration.account.service.dto.OpenAccountResponse;
import com.northbank.registration.shared.security.AuthenticatedCustomer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for bank account management (EPIC-02).
 *
 * <p>All endpoints require a valid JWT Bearer token.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Bank account management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    // ── US-006: Open account ───────────────────────────────────────────

    @Operation(
        summary     = "Open a new bank account",
        description = "Creates a CHECKING or SAVINGS account for the authenticated customer. " +
                      "A customer may hold at most one of each type. Returns 409 on duplicate."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description  = "Account opened successfully",
            headers      = @Header(
                name        = "Location",
                description = "URI of the newly created account resource",
                schema      = @Schema(type = "string", format = "uri")
            ),
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema    = @Schema(implementation = OpenAccountResponse.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Validation error — type field missing or invalid",
            content = @Content(mediaType = "application/problem+json",
                               schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "401", description = "Unauthenticated request",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "409", description = "Account of this type already exists (AC5)",
            content = @Content(mediaType = "application/problem+json",
                               schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OpenAccountResponse> openAccount(
            @Valid @RequestBody OpenAccountRequest request,
            @AuthenticationPrincipal Object principal) {

        UUID customerId = AuthenticatedCustomer.resolveId(principal);
        log.debug("Open account request: type={}, customerId={}", request.type(), customerId);

        OpenAccountResponse response = accountService.openAccount(customerId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    // ── US-007: List accounts ────────────────────────────────────────

    @Operation(
        summary     = "List all accounts",
        description = "Returns all bank accounts owned by the authenticated customer, newest first."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "Account list returned (may be empty)",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array     = @ArraySchema(schema = @Schema(implementation = AccountSummaryResponse.class))
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthenticated request",
            content = @Content(mediaType = "application/problem+json"))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountSummaryResponse>> listAccounts(
            @AuthenticationPrincipal Object principal) {

        UUID customerId = AuthenticatedCustomer.resolveId(principal);
        log.debug("List accounts request: customerId={}", customerId);

        return ResponseEntity.ok(accountService.listAccounts(customerId));
    }

    // ── US-008: Get account detail ─────────────────────────────────────

    @Operation(
        summary     = "Get account details",
        description = "Returns full details of a specific account. " +
                      "Returns 403 if the account belongs to a different customer, 404 if not found."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "Account details returned",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema    = @Schema(implementation = AccountDetailResponse.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthenticated request (AC5)",
            content = @Content(mediaType = "application/problem+json")),
        @ApiResponse(responseCode = "403", description = "Account belongs to a different customer (AC3)",
            content = @Content(mediaType = "application/problem+json",
                               schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(responseCode = "404", description = "Account not found (AC4)",
            content = @Content(mediaType = "application/problem+json",
                               schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountDetailResponse> getAccountDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal Object principal) {

        UUID customerId = AuthenticatedCustomer.resolveId(principal);
        log.debug("Get account detail: id={}, customerId={}", id, customerId);

        return ResponseEntity.ok(accountService.getAccountDetail(customerId, id));
    }
}
