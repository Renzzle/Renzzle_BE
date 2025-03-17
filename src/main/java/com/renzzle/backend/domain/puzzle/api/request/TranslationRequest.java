package com.renzzle.backend.domain.puzzle.api.request;

import jakarta.validation.constraints.NotBlank;

public record TranslationRequest(

        @NotBlank(message = "packId는 필수입니다")
        Long packId,

        @NotBlank(message = "langCode는 필수입니다")
        String langCode,

        @NotBlank(message = "title은 필수입니다")
        String title,

        @NotBlank(message = "author는 필수입니다")
        String author,

        String description
) { }
