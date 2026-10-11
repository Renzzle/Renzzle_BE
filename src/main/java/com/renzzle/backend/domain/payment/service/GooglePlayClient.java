package com.renzzle.backend.domain.payment.service;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

// Google Play Developer API calls for one-time products
@Service
public class GooglePlayClient {

    private static final String ANDROID_PUBLISHER_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
    private static final String PRODUCT_PURCHASE_PATH =
            "/androidpublisher/v3/applications/{packageName}/purchases/products/{productId}/tokens/{purchaseToken}";

    private final RestClient restClient;
    private final String packageName;
    private final String credentialsPath;
    private GoogleCredentials credentials;

    @Autowired
    public GooglePlayClient(
            RestClient.Builder restClientBuilder,
            @Value("${iap.google.package-name}") String packageName,
            @Value("${iap.google.credentials-path}") String credentialsPath
    ) {
        this(restClientBuilder, packageName, credentialsPath, null);
    }

    // Tests pass credentials directly instead of a service account key file
    GooglePlayClient(
            RestClient.Builder restClientBuilder,
            String packageName,
            String credentialsPath,
            GoogleCredentials credentials
    ) {
        this.restClient = restClientBuilder
                .baseUrl("https://androidpublisher.googleapis.com")
                .build();
        this.packageName = packageName;
        this.credentialsPath = credentialsPath;
        this.credentials = credentials;
    }

    public String getProductPurchase(String productId, String purchaseToken) {
        return restClient.get()
                .uri(PRODUCT_PURCHASE_PATH, packageName, productId, purchaseToken)
                .headers(headers -> headers.setBearerAuth(issueAccessToken()))
                .retrieve()
                .body(String.class);
    }

    public void acknowledgeProductPurchase(String productId, String purchaseToken) {
        restClient.post()
                .uri(PRODUCT_PURCHASE_PATH + ":acknowledge", packageName, productId, purchaseToken)
                .headers(headers -> headers.setBearerAuth(issueAccessToken()))
                .retrieve()
                .toBodilessEntity();
    }

    private String issueAccessToken() {
        try {
            GoogleCredentials loadedCredentials = getCredentials();
            loadedCredentials.refreshIfExpired();
            return loadedCredentials.getAccessToken().getTokenValue();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to issue a Google Play access token", e);
        }
    }

    private synchronized GoogleCredentials getCredentials() throws IOException {
        if (credentials == null) {
            try (FileInputStream credentialsStream = new FileInputStream(credentialsPath)) {
                credentials = GoogleCredentials.fromStream(credentialsStream)
                        .createScoped(List.of(ANDROID_PUBLISHER_SCOPE));
            }
        }
        return credentials;
    }
}
