package com.renzzle.backend.domain.puzzle.rank.service.dto;

import com.renzzle.backend.domain.puzzle.rank.domain.LatestRankPuzzle;

public record NextPuzzleResult(
        LatestRankPuzzle latestPuzzle,
        double rating
) {}
