package com.renzzle.backend.domain.puzzle.training.api.request;

import jakarta.validation.constraints.NotNull;

public record PurchaseTrainingPackRequest(

        @NotNull(message = "Pack ID is required")
        Long packId
) { }
