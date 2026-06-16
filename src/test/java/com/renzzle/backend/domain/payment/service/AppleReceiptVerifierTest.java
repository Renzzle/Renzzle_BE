package com.renzzle.backend.domain.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AppleReceiptVerifierTest {

    @DisplayName("운영 URL에서 21007 응답이면 샌드박스 URL로 재시도")
    @Test
    void verify_WhenSandboxReceiptSentToProduction_ThenRetrySandbox() {
        // given
        String productionUrl = "https://buy.itunes.apple.com/verifyReceipt";
        String sandboxUrl = "https://sandbox.itunes.apple.com/verifyReceipt";
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AppleReceiptVerifier verifier = new AppleReceiptVerifier(
                restClientBuilder,
                "shared-secret",
                productionUrl,
                sandboxUrl
        );

        server.expect(requestTo(productionUrl))
                .andRespond(withSuccess("{\"status\":21007}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(sandboxUrl))
                .andRespond(withSuccess("""
                        {
                          "status": 0,
                          "receipt": {
                            "in_app": [
                              {
                                "transaction_id": "transaction-id",
                                "product_id": "piece_1000"
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        StoreVerificationResult result = verifier.verify("piece_1000", "transaction-id", "receipt");

        // then
        assertThat(result.productId()).isEqualTo("piece_1000");
        assertThat(result.transactionId()).isEqualTo("transaction-id");
        server.verify();
    }
}
