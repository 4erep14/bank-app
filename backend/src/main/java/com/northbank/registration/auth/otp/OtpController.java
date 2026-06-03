// Story: US-003
package com.northbank.registration.auth.otp;

import com.northbank.registration.auth.otp.dto.OtpVerifyResponse;
import com.northbank.registration.auth.otp.dto.ResendOtpRequest;
import com.northbank.registration.auth.otp.dto.ResendOtpResponse;
import com.northbank.registration.auth.otp.dto.VerifyOtpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the SMS-OTP second authentication step (ADR-003, US-003).
 *
 * <p>Exposes two public (permitAll) endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/v1/auth/verify-otp} — submit the OTP, receive tokens on success.</li>
 *   <li>{@code POST /api/v1/auth/resend-otp}  — request a fresh OTP (rate-limited).</li>
 * </ul>
 *
 * <p>All business logic is delegated to {@link OtpService}. Exception mapping
 * to RFC 7807 ProblemDetail is handled globally by {@code GlobalExceptionHandler}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Customer authentication endpoints")
public class OtpController {

    private final OtpService otpService;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/verify-otp
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Step-2 authentication: submit the 6-digit OTP to complete the 2FA flow.
     *
     * <ul>
     *   <li>AC2: expired OTP → 401 "Invalid or expired OTP".</li>
     *   <li>AC3: valid OTP → 200 with {@code accessToken} + {@code refreshToken}.</li>
     *   <li>AC4: invalid OTP → 401 with {@code remainingAttempts} extension.</li>
     *   <li>AC5: 3 consecutive failures → session invalidated; further attempts → 401.</li>
     * </ul>
     */
    @Operation(
            summary     = "Verify OTP — step 2 of 2FA",
            description = "Validates the 6-digit SMS code against the OTP session identified " +
                          "by the SESSION JWT. On success issues a 15-minute access token and " +
                          "a 7-day refresh token. Three consecutive wrong attempts invalidate " +
                          "the session (AC5)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description  = "OTP valid — access and refresh tokens issued (AC3)",
                    content      = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema    = @Schema(implementation = OtpVerifyResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description  = "Validation error — missing or malformed fields",
                    content      = @Content(
                            mediaType = "application/problem+json",
                            schema    = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Invalid or expired OTP (AC2/AC4); session invalidated (AC5)",
                    content      = @Content(
                            mediaType = "application/problem+json",
                            schema    = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping(
            path     = "/verify-otp",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<OtpVerifyResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        OtpVerifyResponse response = otpService.verifyOtp(request.sessionToken(), request.otp());
        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/auth/resend-otp
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resends a fresh OTP to the customer's registered phone number.
     *
     * <p>Rate-limited: at most one resend per 60-second window per session.
     * The session must not be invalidated.</p>
     */
    @Operation(
            summary     = "Resend OTP",
            description = "Generates a new 6-digit OTP and sends it via SMS stub. " +
                          "Rate-limited to one request per 60 seconds per session. " +
                          "Returns 429 with Retry-After: 60 if called too soon."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description  = "New OTP dispatched successfully",
                    content      = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema    = @Schema(implementation = ResendOtpResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description  = "Validation error — sessionToken missing",
                    content      = @Content(
                            mediaType = "application/problem+json",
                            schema    = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "Session token invalid or session is invalidated",
                    content      = @Content(
                            mediaType = "application/problem+json",
                            schema    = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description  = "OTP resent too recently — wait 60 seconds (Retry-After header set)",
                    content      = @Content(
                            mediaType = "application/problem+json",
                            schema    = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping(
            path     = "/resend-otp",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ResendOtpResponse> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        ResendOtpResponse response = otpService.resendOtp(request.sessionToken());
        return ResponseEntity.ok(response);
    }
}
