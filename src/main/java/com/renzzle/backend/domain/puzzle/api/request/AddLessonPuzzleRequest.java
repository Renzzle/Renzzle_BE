package com.renzzle.backend.domain.puzzle.api.request;

import com.renzzle.backend.domain.puzzle.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.domain.WinColor;
import com.renzzle.backend.global.validation.ValidBoardString;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddLessonPuzzleRequest(
        @NotNull(message = "챕터 정보가 없습니다")
        Integer chapter,

        Integer puzzleIndex,

        @NotEmpty(message = "제목 정보가 없습니다")
        @Size(min = 1, max = 30, message = "제목은 1~30 자리의 문자열이어야 합니다")
        String title,

        @NotEmpty(message = "보드 정보가 없습니다")
        @ValidBoardString
        String boardStatus,

        @Size(max = 1023, message = "설명은 1023자 이하로 작성해야 합니다")
        String description,

        @NotNull(message = "깊이 정보가 없습니다")
        Integer depth,

        @NotEmpty(message = "난이도 정보가 없습니다")
        @ValidEnum(enumClass = Difficulty.DifficultyName.class, message = "잘못된 Difficulty 타입입니다")
        String difficulty,

        @NotEmpty(message = "승리 색상 정보가 없습니다")
        @ValidEnum(enumClass = WinColor.WinColorName.class, message = "잘못된 WinColor 타입입니다")
        String winColor
) { }
