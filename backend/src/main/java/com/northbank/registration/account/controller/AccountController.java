// Story: US-006 | US-007
package com.northbank.registration.account.controller;

import com.northbank.registration.account.service.AccountService;
import com.northbank.registration.account.service.dto.AccountSummaryResponse;
import com.northbank.registration.account.service.dto.OpenAccountRequest;
import com.northbank.registration.account.service.dto.OpenAccountResponse;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for bank account management (EPIC-02).
 *
 * <p>All endpoints require a valid JWT Bearer token.</p>
 *
 * <ul>
 *   <li>US-006: {@code POST /api/v1/accounts}  — open a new account</li>
 *   <li>US-007: {@code GET  /api/v1/accounts}  — list accounts & balances</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Bank account management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    // ── US-006: Open account ──────────────────────────────────────────────────

    @Operation(
        summary     = "Open a new bank account",
        description = "Creates a CHECKING or SAVINGS account for the authenticated customer. " +
                      "A customer may hold at most one of each type. " +
                      "Returns 409 with 'Account of this type already exists' on duplicate."
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
        @ApiResponse(
            responseCode = "400",
            description  = "Validation error — type field is missing or not a valid enum value",
            content      = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description  = "Unauthenticated request — valid Bearer JWT required",
            content      = @Content(mediaType = "application/problem+json")
        ),
        @ApiResponse(
            responseCode = "409",
            description  = "Account of this type already exists for the customer (AC5)",
            content      = @Content(mediaType = "application/problem+json",
                                    schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OpenAccountResponse> openAccount(
            @Valid @RequestBody OpenAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID customerId = UUID.fromString(jwt.getSubject());
        log.debug("Open account request: type={}, customerId={}", request.type(), customerId);

        OpenAccountResponse response = accountService.openAccount(customerId, request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    // ── US-007: List accounts ─────────────────────────────────────────────────

    @Operation(
        summary     = "List all accounts",
        description = "Returns all CHECKING and SAVINGS accounts for the authenticated customer. " +
                      "Balances are always formatted to 2 decimal places. " +
                      "Returns an empty array [] when the customer has no accounts (AC4)."
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
        @ApiResponse(
            responseCode = "401",
            description  = "Unauthenticated request — valid Bearer JWT required (AC6)",
            content      = @Content(mediaType = "application/problem+json")
        )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AccountSummaryResponse>> listAccounts(
            @AuthenticationPrincipal Jwt jwt) {

        UUID customerId = UUID.fromString(jwt.getSubject());
        log.debug("List accounts request: customerId={}", customerId);

        return ResponseEntity.ok(accountService.listAccounts(customerId));
    }
}
