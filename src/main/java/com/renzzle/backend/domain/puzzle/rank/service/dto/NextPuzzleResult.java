package com.renzzle.backend.domain.puzzle.rank.service.dto;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;

// The puzzle picked for the next round, before it is recorded as an assignment
public record NextPuzzleResult(
        PuzzleType puzzleType,
        Long puzzleId,
        String boardStatus,
        String answer,
        WinColor winColor,
        double rating
) {}
