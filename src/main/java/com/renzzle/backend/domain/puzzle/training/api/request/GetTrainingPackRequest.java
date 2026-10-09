package com.renzzle.backend.domain.puzzle.training.api.request;

import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.global.common.domain.LangCode;
import com.renzzle.backend.global.validation.ValidEnum;
import jakarta.validation.constraints.NotBlank;

        public record GetTrainingPackRequest(
                @NotBlank(message = "Difficulty is required")
                @ValidEnum(enumClass = Difficulty.DifficultyName.class, message = "Invalid difficulty format")
                String difficulty,

                @ValidEnum(enumClass = LangCode.LangCodeName.class, message = "Invalid lang format")
                String lang
        ) { }
