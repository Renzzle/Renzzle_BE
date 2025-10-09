package com.renzzle.backend.domain.puzzle.training.api.request;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.global.validation.ValidBoardString;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.Size;

public record ModifyTrainingPuzzleRequest(
        Long packId,

        Integer puzzleIndex,

        @ValidBoardString
        String boardStatus,

        @Size(max = 1023, message = "정답은 1023자 이하로 작성해야 합니다")
        String answer,

        Integer depth,

        @ValidEnum(enumClass = WinColor.WinColorName.class, message = "잘못된 WinColor 타입입니다")
        String winColor
) { }