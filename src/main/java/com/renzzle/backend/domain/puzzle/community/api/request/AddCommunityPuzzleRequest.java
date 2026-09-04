package com.renzzle.backend.domain.puzzle.community.api.request;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.global.validation.ValidBoardString;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record AddCommunityPuzzleRequest(
        @NotEmpty(message = "Board is required")
        @ValidBoardString
        String boardStatus,

        @NotEmpty(message = "Answer is required")
        @ValidBoardString
        String answer,

        @NotNull(message = "Depth is required")
        Integer depth,

        @Length(max = 100)
        String description,

        @NotEmpty(message = "Depth is required")
        @ValidEnum(enumClass = WinColor.WinColorName.class, message = "Invalid WinColor type")
        String winColor,

        @NotNull(message = "Verification flag is required")
        Boolean isVerified
) { }
