package com.renzzle.backend.domain.puzzle.cache.api.request;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 퍼즐 캐시 입력 UI용 커뮤니티 문제 목록 조회 파라미터.
 */
public record GetCommunityPuzzlesForCacheRequest(
        @Size(max = 31, message = "닉네임은 최대 31자입니다")
        String authorNickname,

        @ValidEnum(enumClass = WinColor.WinColorName.class, nullable = true, message = "잘못된 stone 형식입니다")
        String stone,

        @Min(value = 1, message = "depthMin은 최소 1이어야 합니다")
        @Max(value = 225, message = "depthMin은 최대 225이어야 합니다")
        Integer depthMin,

        @Min(value = 1, message = "depthMax은 최소 1이어야 합니다")
        @Max(value = 225, message = "depthMax은 최대 225이어야 합니다")
        Integer depthMax,

        Long id,

        @Min(value = 1, message = "size는 최소 1이어야 합니다")
        @Max(value = 100, message = "size는 최대 100이어야 합니다")
        Integer size
) {
}
