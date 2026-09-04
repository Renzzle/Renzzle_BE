package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record AuthEmailRequest(
        @NotEmpty(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
) { }
