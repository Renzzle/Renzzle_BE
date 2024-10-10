package com.renzzle.backend.domain.puzzle.api.request;

import com.renzzle.backend.global.common.constant.SortOption;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record GetCommunityPuzzleRequest(
        Long id,

        @Min(value = 1, message = "size는 최소 1이어야 합니다")
        @Max(value = 100, message = "size는 최대 100이어야 합니다")
        Integer size,

        @ValidEnum(enumClass = SortOption.class, message = "잘못된 sort 형식입니다")
        String sort
) { }
