package com.renzzle.backend.domain.payment.api.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record GetGoogleVoidedPurchaseForAdminResponse(
        String orderId,
        Instant voidedAt,
        Integer voidedSource,
        Integer voidedReason,
        // False when the purchase was never verified here, so nothing was granted
        boolean matched,
        Long purchaseId,
        Long userId,
        String nickname,
        String productId,
        String status,
        int grantedCurrency,
        int currentCurrency,
        // False for refunds caused by a missing acknowledgement before the configured date
        boolean reclaimRewards,
        // Already taken for a refunded purchase, otherwise what the next run would take
        int reclaimCurrency
) {
}
