package com.renzzle.backend.domain.payment.api.response;

import lombok.Builder;

@Builder
public record VerifyInAppPurchaseResponse(
        String platform,
        String productId,
        int grantedCurrency
) {
}
