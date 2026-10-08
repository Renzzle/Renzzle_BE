package com.renzzle.backend.domain.puzzle.training.api.request;

import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTrainingPackRequest(
        @NotNull(message = "Translation info is required")
        List<PackTranslationRequest> info,

        @NotNull(message = "Price is required")
        Integer price,

        @NotBlank(message = "Difficulty is required")
        @ValidEnum(enumClass = Difficulty.DifficultyName.class, message = "Invalid difficulty format")
        String difficulty
) { }
