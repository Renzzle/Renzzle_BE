package com.renzzle.backend.domain.payment.service;

public record StoreVerificationResult(
        String productId,
        String transactionId
) {
}
