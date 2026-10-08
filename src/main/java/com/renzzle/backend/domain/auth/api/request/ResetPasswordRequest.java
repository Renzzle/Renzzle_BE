package com.renzzle.backend.domain.auth.api.request;

import com.renzzle.backend.global.util.EmailUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotEmpty(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotEmpty(message = "Auth verification token is required")
        String authVerityToken,

        @NotEmpty(message = "New password is required")
        @Pattern(regexp = "^(?=.*[A-Za-z])[A-Za-z\\d]{8,}$", message = "Invalid new password format")
        String newPassword
) {
    public ResetPasswordRequest {
        email = EmailUtils.normalize(email);
    }
}
