package com.renzzle.backend.domain.puzzle.training.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTrainingPackRequest(
        @NotNull(message = "info is required")
        List<PackTranslationRequest> info,

        @NotNull(message = "Price is required")
        Integer price,

        @NotBlank(message = "Difficulty is required")
        String difficulty
) { }
