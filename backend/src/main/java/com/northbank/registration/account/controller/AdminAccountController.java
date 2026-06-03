// Story: US-009
package com.northbank.registration.account.controller;

import com.northbank.registration.account.domain.model.AccountStatus;
import com.northbank.registration.account.domain.model.AccountType;
import com.northbank.registration.account.service.AdminAccountService;
import com.northbank.registration.account.service.dto.AdminAccountSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Admin REST controller for viewing and managing all customer accounts (US-009).
 *
 * <p>All endpoints require {@code ROLE_ADMIN} — enforced both at the
 * {@link SecurityConfig} route level and via {@code @PreAuthorize} for
 * defence-in-depth (AC6).</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
@Tag(name = "Admin — Accounts", description = "Admin-only account management endpoints (US-009)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    // ── AC1 + AC3: Paginated list with filters ────────────────────────────────

    @Operation(
            summary     = "List all accounts (paginated)",
            description = "Returns a paginated list of all customer accounts. " +
                          "Supports optional filtering by customerId, type, and status (AC3). " +
                          "Default page size is 20 (AC1). Requires ROLE_ADMIN (AC6)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of account records",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Unauthenticated",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "403", description = "Not an admin (AC6)",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<AdminAccountSummaryResponse>> listAccounts(
            @Parameter(description = "Filter by customer UUID")
            @RequestParam(required = false) UUID customerId,

            @Parameter(description = "Filter by account type (CHECKING or SAVINGS)")
            @RequestParam(required = false) AccountType type,

            @Parameter(description = "Filter by account status (ACTIVE, INACTIVE, CLOSED, FROZEN)")
            @RequestParam(required = false) AccountStatus status,

            @Parameter(description = "Zero-based page index (default 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size (default 20)")
            @RequestParam(defaultValue = "20") int size) {

        log.debug("Admin list accounts: customerId={}, type={}, status={}, page={}, size={}",
                customerId, type, status, page, size);
        return ResponseEntity.ok(
                adminAccountService.listAccounts(customerId, type, status, page, size));
    }

    // ── AC4: Deactivate ───────────────────────────────────────────────────────

    @Operation(
            summary     = "Deactivate an account",
            description = "Sets an ACTIVE account to INACTIVE status (AC4). Requires ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deactivated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdminAccountSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "409", description = "Account already inactive",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "403", description = "Not an admin (AC6)",
                    content = @Content(mediaType = "application/problem+json"))
    })
    @PatchMapping(value = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminAccountSummaryResponse> deactivateAccount(
            @PathVariable UUID id) {
        log.info("Admin deactivate account: id={}", id);
        return ResponseEntity.ok(adminAccountService.deactivateAccount(id));
    }

    // ── AC5: Activate ─────────────────────────────────────────────────────────

    @Operation(
            summary     = "Activate an account",
            description = "Sets an INACTIVE account back to ACTIVE status (AC5). Requires ROLE_ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account activated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdminAccountSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "409", description = "Account is not inactive",
                    content = @Content(mediaType = "application/problem+json")),
            @ApiResponse(responseCode = "403", description = "Not an admin (AC6)",
                    content = @Content(mediaType = "application/problem+json"))
    })
    @PatchMapping(value = "/{id}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminAccountSummaryResponse> activateAccount(
            @PathVariable UUID id) {
        log.info("Admin activate account: id={}", id);
        return ResponseEntity.ok(adminAccountService.activateAccount(id));
    }
}
