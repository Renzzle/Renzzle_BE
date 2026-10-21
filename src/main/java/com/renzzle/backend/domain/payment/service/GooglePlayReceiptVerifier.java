package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

// STORE_VERIFICATION_FAILED only when Google says the purchase is not valid. A call that failed is STORE_API_FAILED
// and a pending payment PURCHASE_PENDING, so the app keeps those purchases and sends them again later
@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePlayReceiptVerifier {

    private static final int PURCHASED = 0;
    private static final int PENDING = 2;

    private final GooglePlayClient googlePlayClient;
    private final ObjectMapper objectMapper;

    public StoreVerificationResult verify(String productId, String purchaseToken) {
        String rawResponse = requestPurchase(productId, purchaseToken);
        GoogleProductPurchaseResponse response = parseResponse(rawResponse);
        if (response.purchaseState() != null && response.purchaseState() == PENDING) {
            throw new CustomException(ErrorCode.PURCHASE_PENDING);
        }
        if (response.purchaseState() == null || response.purchaseState() != PURCHASED) {
            log.warn("Google Play verification failed. rawResponse={}", rawResponse);
            throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
        }

        boolean acknowledgementRequired = response.acknowledgementState() != null
                && response.acknowledgementState() == 0;
        return new StoreVerificationResult(productId, response.orderId(), acknowledgementRequired);
    }

    private String requestPurchase(String productId, String purchaseToken) {
        try {
            return googlePlayClient.getProductPurchase(productId, purchaseToken);
        } catch (RestClientResponseException e) {
            log.warn("Google Play verification failed. status={}, rawResponse={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new CustomException(isRejectedPurchase(e.getStatusCode().value())
                    ? ErrorCode.STORE_VERIFICATION_FAILED
                    : ErrorCode.STORE_API_FAILED);
        } catch (RuntimeException e) {
            log.warn("Google Play verification request failed.", e);
            throw new CustomException(ErrorCode.STORE_API_FAILED);
        }
    }

    // 400 (token not valid for the product) and 410 (token no longer valid) are answers about the purchase.
    // 404 stays a call failure because a wrong package name also gives it, and Google refunds an unfinished
    // purchase after 3 days anyway
    private boolean isRejectedPurchase(int status) {
        return status == 400 || status == 410;
    }

    private GoogleProductPurchaseResponse parseResponse(String rawResponse) {
        try {
            GoogleProductPurchaseResponse response =
                    objectMapper.readValue(rawResponse, GoogleProductPurchaseResponse.class);
            if (response == null) {
                throw new IllegalArgumentException("Empty response");
            }
            return response;
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("Google Play verification response parse failed. rawResponse={}", rawResponse);
            throw new CustomException(ErrorCode.STORE_API_FAILED);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleProductPurchaseResponse(
            Integer purchaseState,
            Integer acknowledgementState,
            String orderId
    ) {
    }
}
