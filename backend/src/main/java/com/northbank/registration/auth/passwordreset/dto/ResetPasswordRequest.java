// Story: US-004
package com.northbank.registration.auth.passwordreset.dto;

import com.northbank.registration.validation.Password;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
    @NotBlank(message = "Reset token is required")
    String token,

    @NotBlank(message = "Password is required")
    @Password
    String newPassword
) {}
