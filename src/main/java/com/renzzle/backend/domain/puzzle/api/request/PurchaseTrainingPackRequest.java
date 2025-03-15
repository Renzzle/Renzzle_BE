package com.renzzle.backend.domain.puzzle.api.request;

import jakarta.validation.constraints.NotNull;

public record PurchaseTrainingPackRequest(

        @NotNull(message = "팩 정보가 없습니다")
        Long packId
) { }
