package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record ConfirmCodeRequest(
        @NotEmpty(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotEmpty(message = "Verification code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Invalid code format")
        String code
) { }
