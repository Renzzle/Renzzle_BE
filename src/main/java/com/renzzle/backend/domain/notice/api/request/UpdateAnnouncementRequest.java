package com.renzzle.backend.domain.notice.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateAnnouncementRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 127, message = "Title must be 127 characters or fewer")
        String title,

        @NotBlank(message = "Context is required")
        @Size(max = 1023, message = "Context must be 1023 characters or fewer")
        String context,

        @NotNull(message = "Expiration time is required")
        Instant expiredAt
) { }
