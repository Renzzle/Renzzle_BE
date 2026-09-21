package com.renzzle.backend.domain.notice.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSystemInfoRequest(
        @NotBlank(message = "Version is required")
        @Size(max = 255, message = "Version must be 255 characters or fewer")
        String version,

        @NotNull(message = "System check flag is required")
        Boolean isSystemCheck
) { }
