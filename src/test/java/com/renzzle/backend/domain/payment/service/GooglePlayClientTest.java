package com.renzzle.backend.domain.payment.service;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GooglePlayClientTest {

    private static final String PRODUCT_PURCHASE_URL = "https://androidpublisher.googleapis.com"
            + "/androidpublisher/v3/applications/com.renzzle_fe/purchases/products/piece_1000/tokens/purchase-token";

    @Test
    void getProductPurchase_ThenSendsAccessTokenAndReturnsRawBody() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GooglePlayClient client = createClient(restClientBuilder);

        server.expect(requestTo(PRODUCT_PURCHASE_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("{\"purchaseState\":0}", MediaType.APPLICATION_JSON));

        // when
        String rawResponse = client.getProductPurchase("piece_1000", "purchase-token");

        // then
        assertThat(rawResponse).isEqualTo("{\"purchaseState\":0}");
        server.verify();
    }

    @Test
    void acknowledgeProductPurchase_ThenPostsToAcknowledgeEndpoint() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GooglePlayClient client = createClient(restClientBuilder);

        server.expect(requestTo(PRODUCT_PURCHASE_URL + ":acknowledge"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withNoContent());

        // when
        client.acknowledgeProductPurchase("piece_1000", "purchase-token");

        // then
        server.verify();
    }

    @Test
    void acknowledgeProductPurchase_WhenGoogleRejects_ThenThrows() {
        // given
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        GooglePlayClient client = createClient(restClientBuilder);

        server.expect(requestTo(PRODUCT_PURCHASE_URL + ":acknowledge"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        // when, then
        assertThrows(RestClientResponseException.class,
                () -> client.acknowledgeProductPurchase("piece_1000", "purchase-token"));
        server.verify();
    }

    private GooglePlayClient createClient(RestClient.Builder restClientBuilder) {
        GoogleCredentials credentials = GoogleCredentials.newBuilder()
                .setAccessToken(new AccessToken("test-token", Date.from(Instant.now().plus(1, ChronoUnit.DAYS))))
                .build();
        return new GooglePlayClient(restClientBuilder, "com.renzzle_fe", "unused-credentials-path", credentials);
    }
}
