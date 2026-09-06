package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppleAppStoreServerClientTest {

    private static final String PRODUCTION_URL = "https://api.storekit.itunes.apple.com";
    private static final String SANDBOX_URL = "https://api.storekit-sandbox.itunes.apple.com";
    private static final String TRANSACTION_ID = "2000000123456789";
    private static final String TRANSACTION_PATH = "/inApps/v1/transactions/" + TRANSACTION_ID;
    private static final String NOT_FOUND_BODY =
            "{\"errorCode\":4040010,\"errorMessage\":\"Transaction id not found.\"}";

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KeyPair keyPair;
    private String privateKeyPath;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        keyPair = generator.generateKeyPair();

        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                        .encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        Path keyFile = tempDir.resolve("apple-iap-key.p8");
        Files.writeString(keyFile, pem);
        privateKeyPath = keyFile.toString();
    }

    @Test
    void fetchTransactionInfo_WhenFoundInProduction_ThenReturnsDecodedTransaction() throws Exception {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AppleAppStoreServerClient client = createClient(restClientBuilder);

        server.expect(requestTo(PRODUCTION_URL + TRANSACTION_PATH))
                .andExpect(request -> {
                    String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                    assertThat(authorization).startsWith("Bearer ");

                    Jws<Claims> jws = Jwts.parser()
                            .verifyWith(keyPair.getPublic())
                            .build()
                            .parseSignedClaims(authorization.substring("Bearer ".length()));
                    assertThat(jws.getHeader().getKeyId()).isEqualTo("key-id");
                    assertThat(jws.getPayload().getIssuer()).isEqualTo("issuer-id");
                    assertThat(jws.getPayload().getAudience()).containsExactly("appstoreconnect-v1");
                    assertThat(jws.getPayload().get("bid", String.class)).isEqualTo("com.renzzle");
                })
                .andRespond(withSuccess(
                        transactionInfoResponse("piece_1000", "Production"),
                        MediaType.APPLICATION_JSON));

        // when
        AppleTransactionInfo transaction = client.fetchTransactionInfo(TRANSACTION_ID);

        // then
        assertThat(transaction.transactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(transaction.productId()).isEqualTo("piece_1000");
        assertThat(transaction.bundleId()).isEqualTo("com.renzzle");
        assertThat(transaction.environment()).isEqualTo("Production");
        assertThat(transaction.revocationDate()).isNull();
        server.verify();
    }

    @Test
    void fetchTransactionInfo_WhenNotFoundInProduction_ThenRetriesSandbox() throws Exception {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AppleAppStoreServerClient client = createClient(restClientBuilder);

        server.expect(requestTo(PRODUCTION_URL + TRANSACTION_PATH))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(NOT_FOUND_BODY));
        server.expect(requestTo(SANDBOX_URL + TRANSACTION_PATH))
                .andRespond(withSuccess(
                        transactionInfoResponse("piece_1000", "Sandbox"),
                        MediaType.APPLICATION_JSON));

        // when
        AppleTransactionInfo transaction = client.fetchTransactionInfo(TRANSACTION_ID);

        // then
        assertThat(transaction.productId()).isEqualTo("piece_1000");
        assertThat(transaction.environment()).isEqualTo("Sandbox");
        server.verify();
    }

    @Test
    void fetchTransactionInfo_WhenNotFoundInBothEnvironments_ThenThrowsCustomException() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AppleAppStoreServerClient client = createClient(restClientBuilder);

        server.expect(requestTo(PRODUCTION_URL + TRANSACTION_PATH))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(NOT_FOUND_BODY));
        server.expect(requestTo(SANDBOX_URL + TRANSACTION_PATH))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(NOT_FOUND_BODY));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> client.fetchTransactionInfo(TRANSACTION_ID));

        // then
        assertEquals(ErrorCode.STORE_VERIFICATION_FAILED, exception.getErrorCode());
        server.verify();
    }

    @Test
    void fetchTransactionInfo_WhenProductionUnauthorized_ThenRetriesSandbox() throws Exception {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AppleAppStoreServerClient client = createClient(restClientBuilder);

        server.expect(requestTo(PRODUCTION_URL + TRANSACTION_PATH))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(SANDBOX_URL + TRANSACTION_PATH))
                .andRespond(withSuccess(
                        transactionInfoResponse("piece_1000", "Sandbox"),
                        MediaType.APPLICATION_JSON));

        // when
        AppleTransactionInfo transaction = client.fetchTransactionInfo(TRANSACTION_ID);

        // then
        assertThat(transaction.productId()).isEqualTo("piece_1000");
        assertThat(transaction.environment()).isEqualTo("Sandbox");
        server.verify();
    }

    @Test
    void fetchTransactionInfo_WhenUnauthorizedInBothEnvironments_ThenThrowsCustomException() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AppleAppStoreServerClient client = createClient(restClientBuilder);

        server.expect(requestTo(PRODUCTION_URL + TRANSACTION_PATH))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        server.expect(requestTo(SANDBOX_URL + TRANSACTION_PATH))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> client.fetchTransactionInfo(TRANSACTION_ID));

        // then
        assertEquals(ErrorCode.STORE_VERIFICATION_FAILED, exception.getErrorCode());
        server.verify();
    }

    @Test
    void fetchTransactionInfo_WhenSignedPayloadMalformed_ThenThrowsCustomException() throws Exception {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AppleAppStoreServerClient client = createClient(restClientBuilder);

        server.expect(requestTo(PRODUCTION_URL + TRANSACTION_PATH))
                .andRespond(withSuccess(
                        objectMapper.writeValueAsString(Map.of("signedTransactionInfo", "not-a-jws")),
                        MediaType.APPLICATION_JSON));

        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> client.fetchTransactionInfo(TRANSACTION_ID));

        // then
        assertEquals(ErrorCode.STORE_VERIFICATION_FAILED, exception.getErrorCode());
        server.verify();
    }

    private AppleAppStoreServerClient createClient(RestClient.Builder restClientBuilder) {
        return new AppleAppStoreServerClient(
                restClientBuilder,
                objectMapper,
                "issuer-id",
                "key-id",
                "com.renzzle",
                privateKeyPath,
                PRODUCTION_URL,
                SANDBOX_URL
        );
    }

    private String transactionInfoResponse(String productId, String environment) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("signedTransactionInfo", fakeSignedTransaction(productId, environment)));
    }

    private String fakeSignedTransaction(String productId, String environment) throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "transactionId", TRANSACTION_ID,
                "originalTransactionId", TRANSACTION_ID,
                "bundleId", "com.renzzle",
                "productId", productId,
                "type", "Consumable",
                "environment", environment,
                "purchaseDate", 1720000000000L,
                "quantity", 1
        ));
        return fakeJws(payload);
    }

    // The client decodes only the payload part, so the header and signature can be placeholders
    private String fakeJws(String payloadJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = encoder.encodeToString("fake-signature".getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + "." + signature;
    }
}
