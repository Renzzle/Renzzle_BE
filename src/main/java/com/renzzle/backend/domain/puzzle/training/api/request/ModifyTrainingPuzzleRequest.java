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

        @Size(max = 1023, message = "Answer must be at most 1023 characters")
        String answer,

        Integer depth,

        @ValidEnum(enumClass = WinColor.WinColorName.class, message = "Invalid WinColor type", nullable = true)
        String winColor
) { }