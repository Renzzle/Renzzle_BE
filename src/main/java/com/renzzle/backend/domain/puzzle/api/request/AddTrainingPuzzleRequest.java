package com.renzzle.backend.domain.puzzle.api.request;

import com.renzzle.backend.domain.puzzle.domain.WinColor;
import com.renzzle.backend.global.validation.ValidBoardString;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddTrainingPuzzleRequest(
        @NotNull(message = "팩 정보가 없습니다")
        Long packId,

        Integer puzzleIndex,

        @NotEmpty(message = "보드 정보가 없습니다")
        @ValidBoardString
        String boardStatus,

        @Size(max = 1023, message = "정답은 1023자 이하로 작성해야 합니다")
        String answer,

        @NotNull(message = "깊이 정보가 없습니다")
        Integer depth,

        @NotEmpty(message = "승리 색상 정보가 없습니다")
        @ValidEnum(enumClass = WinColor.WinColorName.class, message = "잘못된 WinColor 타입입니다")
        String winColor
) { }
