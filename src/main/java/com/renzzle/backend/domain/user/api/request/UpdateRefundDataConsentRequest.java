package com.renzzle.backend.domain.user.api.request;

import jakarta.validation.constraints.NotNull;

public record UpdateRefundDataConsentRequest(
        @NotNull(message = "Consented is required")
        Boolean consented
) { }
