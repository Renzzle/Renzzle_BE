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

@Slf4j
@Service
@RequiredArgsConstructor
public class GooglePlayReceiptVerifier {

    private final GooglePlayClient googlePlayClient;
    private final ObjectMapper objectMapper;

    public StoreVerificationResult verify(String productId, String purchaseToken) {
        try {
            String rawResponse = googlePlayClient.getProductPurchase(productId, purchaseToken);
            GoogleProductPurchaseResponse response = parseResponse(rawResponse);
            if (response == null || response.purchaseState() == null || response.purchaseState() != 0) {
                log.warn("Google Play verification failed. rawResponse={}", rawResponse);
                throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
            }

            boolean acknowledgementRequired = response.acknowledgementState() != null
                    && response.acknowledgementState() == 0;
            return new StoreVerificationResult(productId, response.orderId(), acknowledgementRequired);
        } catch (CustomException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("Google Play verification failed. status={}, rawResponse={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
        } catch (Exception e) {
            log.warn("Google Play verification failed.", e);
            throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
        }
    }

    private GoogleProductPurchaseResponse parseResponse(String rawResponse) {
        try {
            return objectMapper.readValue(rawResponse, GoogleProductPurchaseResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("Google Play verification response parse failed. rawResponse={}", rawResponse);
            throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
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
