package com.renzzle.backend.domain.puzzle.api.request;

public record AddPuzzleRequest(
        String title,
        String boardStatus,
        int depth,
        String difficulty,
        String winColor
) { }
