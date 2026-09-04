package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotEmpty(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotEmpty(message = "Password is required")
        @Pattern(regexp = "^(?=.*[A-Za-z])[A-Za-z\\d]{8,}$", message = "Invalid password format")
        String password,

        @NotEmpty(message = "Nickname is required")
        @Pattern(regexp = "^[\\p{L}0-9]*$", message = "Nickname must not contain special characters")
        @Size(min = 2, max = 8, message = "Nickname must be 2-8 characters")
        String nickname,

        @NotEmpty(message = "Token is required")
        String authVerityToken,

        @NotEmpty(message = "Device ID is required")
        String deviceId
) { }
