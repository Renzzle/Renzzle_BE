package com.renzzle.backend.domain.puzzle.shared.dto;

import java.util.List;

// Each duplicates entry lists puzzles of one position; they keep their keys until only one is left
public record BoardKeyRecalculationResult(
        int updatedCount,
        List<List<Long>> duplicates
) {}
