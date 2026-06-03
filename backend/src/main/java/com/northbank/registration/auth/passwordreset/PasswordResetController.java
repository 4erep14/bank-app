// Story: US-004
package com.northbank.registration.auth.passwordreset;

import com.northbank.registration.auth.passwordreset.dto.ForgotPasswordRequest;
import com.northbank.registration.auth.passwordreset.dto.MessageResponse;
import com.northbank.registration.auth.passwordreset.dto.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Customer authentication endpoints")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Operation(summary = "Request password reset email (AC1, AC2)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Uniform 200 — anti-enumeration"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return passwordResetService.forgotPassword(request);
    }

    @Operation(summary = "Complete password reset using the token from the email link (AC3-AC7)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid/expired/used token or weak password")
    })
    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return passwordResetService.resetPassword(request);
    }
}
