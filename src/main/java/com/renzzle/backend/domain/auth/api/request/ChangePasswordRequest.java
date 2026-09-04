package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotEmpty(message = "Current password is required")
        String currentPassword,

        @NotEmpty(message = "New password is required")
        @Pattern(regexp = "^(?=.*[A-Za-z])[A-Za-z\\d]{8,}$", message = "Invalid new password format")
        String newPassword
) { }
