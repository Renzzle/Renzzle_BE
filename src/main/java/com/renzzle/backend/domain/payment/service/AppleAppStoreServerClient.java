package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Service
public class AppleAppStoreServerClient {

    private static final String APP_STORE_CONNECT_AUDIENCE = "appstoreconnect-v1";
    private static final long TOKEN_VALIDITY_MINUTES = 5;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String issuerId;
    private final String keyId;
    private final String bundleId;
    private final String privateKeyPath;
    private final String productionUrl;
    private final String sandboxUrl;
    private PrivateKey privateKey;

    public AppleAppStoreServerClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${iap.apple.issuer-id}") String issuerId,
            @Value("${iap.apple.key-id}") String keyId,
            @Value("${iap.apple.bundle-id}") String bundleId,
            @Value("${iap.apple.private-key-path}") String privateKeyPath,
            @Value("${iap.apple.production-url}") String productionUrl,
            @Value("${iap.apple.sandbox-url}") String sandboxUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.issuerId = issuerId;
        this.keyId = keyId;
        this.bundleId = bundleId;
        this.privateKeyPath = privateKeyPath;
        this.productionUrl = productionUrl;
        this.sandboxUrl = sandboxUrl;
    }

    public AppleTransactionInfo fetchTransactionInfo(String transactionId) {
        try {
            String token = generateToken();
            String signedTransactionInfo = requestFromProductionOrNull(transactionId, token);
            if (signedTransactionInfo == null) {
                // Sandbox transactions are not visible in production (Apple: try production, then sandbox)
                signedTransactionInfo = requestSignedTransactionInfo(sandboxUrl, transactionId, token);
            }

            if (signedTransactionInfo == null) {
                log.warn("Apple transaction not found in both environments. transactionId={}", transactionId);
                throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
            }

            return decodeTransactionInfo(signedTransactionInfo);
        } catch (CustomException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.warn("App Store Server API request failed. status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
        } catch (RuntimeException e) {
            log.warn("App Store Server API request failed.", e);
            throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
        }
    }

    private String requestFromProductionOrNull(String transactionId, String token) {
        try {
            return requestSignedTransactionInfo(productionUrl, transactionId, token);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != HttpStatus.UNAUTHORIZED.value()) {
                throw e;
            }
            log.info("Production App Store returned 401 (app likely unreleased); retrying sandbox. "
                    + "transactionId={}", transactionId);
            return null;
        }
    }

    private String requestSignedTransactionInfo(String baseUrl, String transactionId, String token) {
        try {
            TransactionInfoResponse response = restClient.get()
                    .uri(baseUrl + "/inApps/v1/transactions/{transactionId}", transactionId)
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(TransactionInfoResponse.class);
            return response == null ? null : response.signedTransactionInfo();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return null;
            }
            throw e;
        }
    }

    @SuppressWarnings("deprecation")
    private String generateToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(keyId).type("JWT").and()
                .issuer(issuerId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(TOKEN_VALIDITY_MINUTES, ChronoUnit.MINUTES)))
                // Apple requires the aud claim as a plain string, not a JSON array
                .audience().single(APP_STORE_CONNECT_AUDIENCE)
                .claim("bid", bundleId)
                .signWith(loadPrivateKey(), Jwts.SIG.ES256)
                .compact();
    }

    private synchronized PrivateKey loadPrivateKey() {
        if (privateKey == null) {
            try {
                String pem = Files.readString(Path.of(privateKeyPath), StandardCharsets.UTF_8);
                String base64Key = pem
                        .replace("-----BEGIN PRIVATE KEY-----", "")
                        .replace("-----END PRIVATE KEY-----", "")
                        .replaceAll("\\s", "");
                byte[] encodedKey = Base64.getDecoder().decode(base64Key);
                privateKey = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException
                     | IllegalArgumentException e) {
                log.warn("Failed to load the Apple IAP private key. path={}", privateKeyPath, e);
                throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
            }
        }
        return privateKey;
    }

    // The JWS payload is trusted as-is because it is fetched from Apple directly over TLS
    private AppleTransactionInfo decodeTransactionInfo(String signedTransactionInfo) {
        try {
            String[] parts = signedTransactionInfo.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWS format");
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(payload, AppleTransactionInfo.class);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode JWS payload", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TransactionInfoResponse(
            String signedTransactionInfo
    ) {
    }
}
