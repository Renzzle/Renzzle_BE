package com.renzzle.backend.domain.puzzle.shared.dto;

import java.util.List;

// invalidIds are puzzles whose board or answer cannot be parsed; they are left without a key
public record AnswerKeyRecalculationResult(
        int updatedCount,
        List<Long> invalidIds
) {}
