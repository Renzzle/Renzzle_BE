package com.renzzle.backend.domain.puzzle.cache.api.request;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SavePuzzleRequest(
        @NotNull(message = "퍼즐 타입이 존재해야 합니다.")
        PuzzleType puzzleType,

        @NotNull(message = "퍼즐 ID가 존재해야 합니다.")
        Long puzzleId,

        @NotBlank(message = "보드 상태가 존재해야 합니다.")
        String currentBoardState,

        @NotBlank(message = "정답 수가 존재해야 합니다.")
        String answerPuzzle
) { }
