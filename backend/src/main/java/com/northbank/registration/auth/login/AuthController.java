// Story: US-002
package com.northbank.registration.auth.login;

import com.northbank.registration.auth.login.dto.LoginRequest;
import com.northbank.registration.auth.login.dto.LoginResponse;
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
 * REST controller for the first authentication step — customer login (ADR-002).
 *
 * <p>Exposes {@code POST /api/v1/auth/login} as a public (anonymous) endpoint.
 * All business logic, including the account-lockout algorithm, is delegated
 * to {@link AuthService}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Customer authentication endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Step-1 authentication: validate email + password and initiate the 2FA flow.
     *
     * <ul>
     *   <li>AC1: accepts email + password via POST body.</li>
     *   <li>AC2: on valid credentials returns 200 with {@code status="2FA_REQUIRED"} and a
     *       5-minute signed {@code sessionToken}.</li>
     *   <li>AC3: invalid credentials → 401.</li>
     *   <li>AC4: 5th consecutive failure locks the account → 423.</li>
     *   <li>AC5: locked account → 423 regardless of password.</li>
     * </ul>
     */
    @Operation(
            summary     = "Customer login — step 1 of 2FA",
            description = "Validates email and password. On success returns a short-lived SESSION JWT " +
                          "and instructs the client to proceed to OTP verification (US-003). " +
                          "Five consecutive failures lock the account (AC4)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description  = "Credentials valid — 2FA step required",
                    content      = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema    = @Schema(implementation = LoginResponse.class)
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
                    description  = "Invalid email or password (AC3)",
                    content      = @Content(
                            mediaType = "application/problem+json",
                            schema    = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "423",
                    description  = "Account locked due to too many failed attempts (AC4/AC5)",
                    content      = @Content(
                            mediaType = "application/problem+json",
                            schema    = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping(
            path     = "/login",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
