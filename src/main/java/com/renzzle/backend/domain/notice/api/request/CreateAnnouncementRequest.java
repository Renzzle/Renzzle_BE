package com.renzzle.backend.domain.notice.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record CreateAnnouncementRequest(
        @NotNull(message = "Expiration time is required")
        Instant expiredAt,

        @NotEmpty(message = "At least one language content is required")
        @Valid
        List<AnnouncementContentRequest> contents
) { }
