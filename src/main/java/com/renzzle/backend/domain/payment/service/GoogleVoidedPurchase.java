package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

// Google Play Developer API VoidedPurchase: a purchase that was refunded, canceled or charged back
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleVoidedPurchase(
        String purchaseToken,
        String orderId,
        Long voidedTimeMillis,
        // 0 user, 1 developer, 2 Google
        Integer voidedSource,
        // 0 other, 1 remorse, 2 not received, 3 defective, 4 accidental, 5 fraud, 6 friendly fraud,
        // 7 chargeback, 8 unacknowledged purchase
        Integer voidedReason
) {

    // Google refunded it because nobody acknowledged the purchase within 3 days
    public static final int UNACKNOWLEDGED_PURCHASE = 8;

    public boolean isUnacknowledgedPurchase() {
        return voidedReason != null && voidedReason == UNACKNOWLEDGED_PURCHASE;
    }

    public Instant voidedAt() {
        return voidedTimeMillis == null ? null : Instant.ofEpochMilli(voidedTimeMillis);
    }
}
