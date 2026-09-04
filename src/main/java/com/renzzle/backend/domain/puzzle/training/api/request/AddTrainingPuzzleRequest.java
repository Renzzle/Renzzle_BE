package com.renzzle.backend.domain.puzzle.training.api.request;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.global.validation.ValidBoardString;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddTrainingPuzzleRequest(
        @NotNull(message = "Pack is required")
        Long packId,

        @NotNull
        Integer puzzleIndex,

        @NotEmpty(message = "Board is required")
        @ValidBoardString
        String boardStatus,

        @NotEmpty(message = "Answer is required")
        @Size(max = 1023, message = "Answer must be at most 1023 characters")
        String answer,

        @NotNull(message = "Depth is required")
        Integer depth,

        @NotEmpty(message = "Win color is required")
        @ValidEnum(enumClass = WinColor.WinColorName.class, message = "Invalid WinColor type")
        String winColor
) { }
