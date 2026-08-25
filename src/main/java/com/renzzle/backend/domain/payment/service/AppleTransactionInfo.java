package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Decoded payload of a signed transaction (JWSTransactionDecodedPayload) from the App Store Server API
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppleTransactionInfo(
        String transactionId,
        String originalTransactionId,
        String bundleId,
        String productId,
        String type,
        String environment,
        Long purchaseDate,
        Integer quantity,
        String inAppOwnershipType,
        String transactionReason,
        Long revocationDate,
        Integer revocationReason
) {
}
