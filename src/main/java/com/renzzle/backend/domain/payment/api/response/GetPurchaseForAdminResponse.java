package com.renzzle.backend.domain.payment.api.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record GetPurchaseForAdminResponse(
        Long purchaseId,
        Long userId,
        String email,
        String nickname,
        String platform,
        String productId,
        // Google order ID or App Store transaction ID
        String transactionId,
        String status,
        int grantedCurrency,
        int currentCurrency,
        Instant purchasedAt,
        Instant withdrawalDeadline,
        boolean withinWithdrawalPeriod,
        boolean unused,
        boolean refundable,
        Instant refundedAt,
        String refundReason,
        int reclaimedCurrency
) {
}
