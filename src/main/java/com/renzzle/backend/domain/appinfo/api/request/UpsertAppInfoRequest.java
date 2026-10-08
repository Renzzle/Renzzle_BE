package com.renzzle.backend.domain.appinfo.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertAppInfoRequest(
        @NotBlank(message = "Tag is required")
        @Size(max = 127, message = "Tag must be 127 characters or fewer")
        String tag,

        @NotBlank(message = "Value is required")
        @Size(max = 2047, message = "Value must be 2047 characters or fewer")
        String value
) { }
