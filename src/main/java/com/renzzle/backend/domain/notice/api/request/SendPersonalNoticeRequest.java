package com.renzzle.backend.domain.notice.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendPersonalNoticeRequest(
        @NotNull(message = "User ID is required")
        Long userId,

        @NotBlank(message = "Context is required")
        @Size(max = 255, message = "Context must be 255 characters or fewer")
        String context
) { }
