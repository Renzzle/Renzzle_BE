package com.renzzle.backend.domain.puzzle.shared.dto;

public record RankPuzzleCandidate(
        Long id,
        String boardStatus,
        String answer,
        String winColor,
        Double rating
) {
}
