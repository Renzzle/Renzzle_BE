package com.renzzle.backend.domain.puzzle.community.api.request;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.global.common.constant.SortOption;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GetCommunityPuzzleRequest(
        Long id,

        @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must be at most 100")
        Integer size,

        @ValidEnum(enumClass = SortOption.class, nullable = true, message = "Invalid sort format")
        String sort,
        
        Long shuffleSeed,

        @ValidEnum(enumClass = WinColor.WinColorName.class, nullable = true, message = "Invalid stone format")
        String stone,

        Boolean auth,

        @Min(value = 1, message = "depthMin must be at least 1")
        @Max(value = 225, message = "depthMin must be at most 225")
        Integer depthMin,

        @Min(value = 1, message = "depthMax must be at least 1")
        @Max(value = 225, message = "depthMax must be at most 225")
        Integer depthMax,

        Boolean solved,

        @Size(min = 1, max = 10, message = "Search term must be 1-10 characters")
        String query
) { }
