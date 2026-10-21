package com.renzzle.backend.domain.payment.service;

public record StoreVerificationResult(
        String productId,
        String transactionId,
        // Google only: Google refunds a purchase that nobody acknowledges within 3 days
        boolean acknowledgementRequired
) {

    public StoreVerificationResult(String productId, String transactionId) {
        this(productId, transactionId, false);
    }
}
