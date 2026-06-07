package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AppleReceiptVerifier {

    private static final int VALID_RECEIPT_STATUS = 0;
    private static final int SANDBOX_RECEIPT_SENT_TO_PRODUCTION = 21007;

    private final RestClient restClient;
    private final String sharedSecret;
    private final String productionUrl;
    private final String sandboxUrl;

    public AppleReceiptVerifier(
            RestClient.Builder restClientBuilder,
            @Value("${iap.apple.shared-secret}") String sharedSecret,
            @Value("${iap.apple.production-url}") String productionUrl,
            @Value("${iap.apple.sandbox-url}") String sandboxUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.sharedSecret = sharedSecret;
        this.productionUrl = productionUrl;
        this.sandboxUrl = sandboxUrl;
    }

    public StoreVerificationResult verify(String productId, String transactionId, String receipt) {
        try {
            AppleVerifyReceiptResponse response = requestVerifyReceipt(productionUrl, receipt);
            if (response != null && response.status() == SANDBOX_RECEIPT_SENT_TO_PRODUCTION) {
                response = requestVerifyReceipt(sandboxUrl, receipt);
            }

            if (response == null || response.status() != VALID_RECEIPT_STATUS) {
                throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
            }

            AppleInAppPurchase matchedPurchase = findMatchedPurchase(response, productId, transactionId);

            return new StoreVerificationResult(
                    matchedPurchase.productId(),
                    matchedPurchase.transactionId()
            );
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
        }
    }

    private AppleVerifyReceiptResponse requestVerifyReceipt(String url, String receipt) {
        Map<String, String> requestBody = Map.of(
                "receipt-data", receipt,
                "password", sharedSecret
        );

        return restClient.post()
                .uri(url)
                .body(requestBody)
                .retrieve()
                .body(AppleVerifyReceiptResponse.class);
    }

    private AppleInAppPurchase findMatchedPurchase(
            AppleVerifyReceiptResponse response,
            String productId,
            String transactionId
    ) {
        List<AppleInAppPurchase> purchases = response.receipt() == null
                ? List.of()
                : response.receipt().inApp();

        if (purchases == null) {
            purchases = List.of();
        }

        return purchases.stream()
                .filter(purchase -> transactionId.equals(purchase.transactionId()))
                .filter(purchase -> productId.equals(purchase.productId()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.RECEIPT_TRANSACTION_MISMATCH));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AppleVerifyReceiptResponse(
            int status,
            AppleReceipt receipt
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AppleReceipt(
            @JsonProperty("in_app")
            List<AppleInAppPurchase> inApp
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AppleInAppPurchase(
            @JsonProperty("transaction_id")
            String transactionId,

            @JsonProperty("product_id")
            String productId
    ) {
    }
}
