package com.renzzle.backend.domain.user.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeNicknameRequest(
        @NotEmpty(message = "Nickname is required")
        @Pattern(regexp = "^[\\p{L}0-9]*$", message = "Nickname must not contain special characters")
        @Size(min = 2, max = 8, message = "Nickname must be 2-8 characters")
        String nickname
) { }
