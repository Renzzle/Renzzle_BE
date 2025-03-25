package com.renzzle.backend.domain.puzzle.training.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTrainingPackRequest(
        @NotNull(message = "info가 필요합니다")
        List<PackTranslationRequest> info,

        @NotNull(message = "가격이 필요합니다")
        Integer price,

        @NotBlank(message = "난이도가 필요합니다")
        String difficulty
) { }
