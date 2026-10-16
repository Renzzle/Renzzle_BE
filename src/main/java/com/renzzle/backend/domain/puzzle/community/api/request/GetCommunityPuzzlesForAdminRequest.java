package com.renzzle.backend.domain.puzzle.community.api.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetCommunityPuzzlesForAdminRequest(
        Boolean verified,

        @Min(value = 0, message = "page must be at least 0")
        Integer page,

        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 50, message = "size must be at most 50")
        Integer size
) { }
