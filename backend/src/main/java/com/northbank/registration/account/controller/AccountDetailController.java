// Story: US-008
package com.northbank.registration.account.controller;

import com.northbank.registration.account.service.AccountDetailService;
import com.northbank.registration.account.service.dto.AccountDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for retrieving a single account's full details (US-008).
 *
 * <p>All endpoints require a valid JWT Bearer token (AC5).
 * The authenticated customer may only access their own accounts (AC3).</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Bank account management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AccountDetailController {

    private final AccountDetailService accountDetailService;

    /**
     * Retrieve full details of a specific account (US-008, AC1).
     */
    @Operation(
            summary     = "Get account details",
            description = "Returns full details of the specified account. " +
                          "Returns 403 if the account belongs to a different customer, " +
                          "404 if the account ID does not exist."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description  = "Account details returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema    = @Schema(implementation = AccountDetailResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Unauthenticated request (AC5)",
                    content      = @Content(mediaType = "application/problem+json")
            ),
            @ApiResponse(
                    responseCode = "403",
                    description  = "Account belongs to a different customer (AC3)",
                    content      = @Content(
                            mediaType = "application/problem+json",
                            schema    = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description  = "Account not found (AC4)",
                    content      = @Content(
                            mediaType = "application/problem+json",
                            schema    = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountDetailResponse> getAccountDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        UUID customerId = UUID.fromString(jwt.getSubject());
        log.debug("Get account detail: id={}, customerId={}", id, customerId);

        return ResponseEntity.ok(accountDetailService.getAccountDetail(customerId, id));
    }
}
