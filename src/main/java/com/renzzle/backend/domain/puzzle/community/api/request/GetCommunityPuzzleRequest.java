package com.renzzle.backend.domain.puzzle.community.api.request;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.global.common.constant.SortOption;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record GetCommunityPuzzleRequest(
        Long id,

        @Min(value = 1, message = "size는 최소 1이어야 합니다")
        @Max(value = 100, message = "size는 최대 100이어야 합니다")
        Integer size,

        @ValidEnum(enumClass = SortOption.class, message = "잘못된 sort 형식입니다")
        String sort,

        @ValidEnum(enumClass = WinColor.WinColorName.class, message = "잘못된 stone 형식입니다")
        String stone,

        Boolean auth,

        @Min(value = 1, message = "depthMin은 최소 1이어야 합니다")
        @Max(value = 225, message = "depthMin은 최대 225이어야 합니다")
        Integer depthMin,

        @Min(value = 1, message = "depthMax은 최소 1이어야 합니다")
        @Max(value = 225, message = "depthMax은 최대 225이어야 합니다")
        Integer depthMax,

        Boolean solved,

        @Size(min = 1, max = 10, message = "검색어는 1글자 이상, 최대 10글자까지 가능합니다")
        String query
) { }
