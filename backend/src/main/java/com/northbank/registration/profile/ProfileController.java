// Story: US-005
package com.northbank.registration.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for the authenticated customer profile endpoints (US-005).
 *
 * <p>Both endpoints require a valid ACCESS JWT supplied in the
 * {@code Authorization: Bearer <token>} header. Authentication is enforced
 * upstream by {@code JwtAuthenticationFilter} — unauthenticated requests never
 * reach this controller (AC6).</p>
 *
 * <p>The {@code @AuthenticationPrincipal UUID customerId} parameter is resolved
 * from {@code Authentication.getPrincipal()}, which the filter populates with the
 * customer UUID parsed from the JWT {@code sub} claim.</p>
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Authenticated customer profile endpoints (US-005)")
public class ProfileController {

    private final ProfileService profileService;

    // ── GET /api/v1/profile ─────────────────────────────────────────────────

    @Operation(
        summary     = "Get authenticated customer's profile",
        description = "Returns firstName, lastName, email, phoneNumber and dateOfBirth " +
                      "for the currently authenticated customer. email and dateOfBirth " +
                      "are read-only (AC3).",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "Profile returned successfully",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema    = @Schema(implementation = ProfileResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description  = "Missing, invalid or expired ACCESS token (AC6)",
            content      = @Content(
                mediaType = "application/problem+json",
                schema    = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ProfileResponse getProfile(@AuthenticationPrincipal UUID customerId) {
        return profileService.getProfile(customerId);
    }

    // ── PATCH /api/v1/profile ───────────────────────────────────────────────

    @Operation(
        summary     = "Update authenticated customer's profile",
        description = "Partially updates firstName, lastName and/or phoneNumber. " +
                      "All fields are optional — omitted fields are left unchanged. " +
                      "Sending email or dateOfBirth returns 400 'Field is not editable' (AC3). " +
                      "phoneNumber must be in E.164 format (AC4).",
        security    = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description  = "Profile updated successfully — full updated profile returned (AC2, AC5)",
            content      = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema    = @Schema(implementation = ProfileResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description  = "Validation error (AC4) or attempt to update read-only field (AC3)",
            content      = @Content(
                mediaType = "application/problem+json",
                schema    = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description  = "Missing, invalid or expired ACCESS token (AC6)",
            content      = @Content(
                mediaType = "application/problem+json",
                schema    = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    @PatchMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ProfileResponse updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UUID customerId) {
        return profileService.updateProfile(customerId, request);
    }
}
