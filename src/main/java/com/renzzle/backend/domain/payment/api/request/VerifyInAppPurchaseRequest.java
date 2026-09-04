package com.renzzle.backend.domain.payment.api.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyInAppPurchaseRequest(
        @NotBlank(message = "Platform is required")
        String platform,

        @NotBlank(message = "Product is required")
        String productId,

        String transactionId,
        String purchaseToken,
        String receipt
) {
}
