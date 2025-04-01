package com.renzzle.backend.domain.puzzle.community.api.request;

import com.renzzle.backend.domain.puzzle.shared.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.global.validation.ValidBoardString;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

public record AddCommunityPuzzleRequest(
        @NotEmpty(message = "보드 정보가 없습니다")
        @ValidBoardString
        String boardStatus,

        @NotEmpty(message = "정답 정보가 없습니다")
        @ValidBoardString
        String answer,

        @NotNull(message = "깊이 정보가 없습니다")
        Integer depth,

        @Length(max = 100)
        String description,

        @NotEmpty(message = "깊이 정보가 없습니다")
        @ValidEnum(enumClass = WinColor.WinColorName.class, message = "잘못된 WinColor 타입입니다")
        String winColor
) { }
