package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Service
public class GooglePlayReceiptVerifier {

    private static final String ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String packageName;
    private final String credentialsPath;

    public GooglePlayReceiptVerifier(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${iap.google.package-name}") String packageName,
            @Value("${iap.google.credentials-path}") String credentialsPath
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://androidpublisher.googleapis.com")
                .build();
        this.objectMapper = objectMapper;
        this.packageName = packageName;
        this.credentialsPath = credentialsPath;
    }

    public StoreVerificationResult verify(String productId, String purchaseToken) {
        try {
            String accessToken = issueAccessToken();
            ResponseEntity<String> responseEntity = restClient.get()
                    .uri(
                            "/androidpublisher/v3/applications/{packageName}/purchases/products/{productId}/tokens/{purchaseToken}",
                            packageName,
                            productId,
                            purchaseToken
                    )
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .toEntity(String.class);

            String rawResponse = responseEntity.getBody();
            GoogleProductPurchaseResponse response = parseResponse(rawResponse);
            if (response == null || response.purchaseState() == null || response.purchaseState() != 0) {
                throw new CustomException("Google Play verification failed. rawResponse=" + rawResponse,
                        ErrorCode.STORE_VERIFICATION_FAILED);
            }

            return new StoreVerificationResult(productId, response.orderId());
        } catch (CustomException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new CustomException(
                    "Google Play verification failed. status=" + e.getStatusCode().value()
                            + ", rawResponse=" + e.getResponseBodyAsString(),
                    ErrorCode.STORE_VERIFICATION_FAILED
            );
        } catch (Exception e) {
            throw new CustomException("Google Play verification failed. reason=" + e.getMessage(),
                    ErrorCode.STORE_VERIFICATION_FAILED);
        }
    }

    private GoogleProductPurchaseResponse parseResponse(String rawResponse) {
        try {
            return objectMapper.readValue(rawResponse, GoogleProductPurchaseResponse.class);
        } catch (JsonProcessingException e) {
            throw new CustomException("Google Play verification response parse failed. rawResponse=" + rawResponse,
                    ErrorCode.STORE_VERIFICATION_FAILED);
        }
    }

    private String issueAccessToken() throws IOException {
        try (FileInputStream credentialsStream = new FileInputStream(credentialsPath)) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(List.of(ANDROID_PUBLISHER_SCOPE));
            credentials.refreshIfExpired();
            return credentials.getAccessToken().getTokenValue();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleProductPurchaseResponse(
            Integer purchaseState,
            String orderId
    ) {
    }
}
