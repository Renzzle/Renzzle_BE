package com.renzzle.backend.domain.puzzle.api.request;

import com.renzzle.backend.domain.puzzle.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.domain.WinColor;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommunityPuzzleRequest(
        @NotBlank(message = "제목 정보가 없습니다")
        @Size(min = 1, max = 30, message = "제목은 1~30 자리의 문자열이어야 합니다")
        String title,

        @NotBlank(message = "보드 정보가 없습니다")
        String boardStatus,

        @NotBlank(message = "깊이 정보가 없습니다")
        int depth,

        @NotBlank(message = "깊이 정보가 없습니다")
        @ValidEnum(enumClass = Difficulty.DifficultyName.class, message = "잘못된 Difficulty 타입입니다")
        String difficulty,

        @NotBlank(message = "깊이 정보가 없습니다")
        @ValidEnum(enumClass = WinColor.WinColorName.class, message = "잘못된 WinColor 타입입니다")
        String winColor
) { }
