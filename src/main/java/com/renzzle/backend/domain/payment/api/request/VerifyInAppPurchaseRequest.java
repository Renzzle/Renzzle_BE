package com.renzzle.backend.domain.payment.api.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyInAppPurchaseRequest(
        @NotBlank(message = "플랫폼 정보가 없습니다")
        String platform,

        @NotBlank(message = "상품 정보가 없습니다")
        String productId,

        String transactionId,
        String purchaseToken,
        String receipt
) {
}
