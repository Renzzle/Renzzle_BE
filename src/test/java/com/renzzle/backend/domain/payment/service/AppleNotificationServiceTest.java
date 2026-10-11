package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.domain.payment.dao.InAppPurchaseRepository;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.payment.domain.PaymentPlatform;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppleNotificationServiceTest {

    private static final String BUNDLE_ID = "com.renzzle";
    private static final String TRANSACTION_ID = "2000000123456789";
    private static final Instant NOW = Instant.parse("2026-10-11T00:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AppleAppStoreServerClient appStoreServerClient;

    @Mock
    private InAppPurchaseRepository inAppPurchaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefundService refundService;

    private AppleNotificationService appleNotificationService;

    @BeforeEach
    void setUp() {
        RefundPolicy refundPolicy = new RefundPolicy(Clock.fixed(NOW, ZoneOffset.UTC), 7);
        appleNotificationService = new AppleNotificationService(appStoreServerClient, inAppPurchaseRepository,
                userRepository, refundService, refundPolicy, objectMapper, BUNDLE_ID);
    }

    @Test
    void handle_WhenRefund_ThenReclaimsWithWhatAppleReports() throws Exception {
        // given
        long revokedAt = NOW.minusSeconds(60).toEpochMilli();
        stubPurchase(purchase(user(0, false), NOW));
        when(appStoreServerClient.fetchTransactionInfo(TRANSACTION_ID)).thenReturn(transaction(revokedAt, 1));

        // when
        appleNotificationService.handle(notification("REFUND", BUNDLE_ID));

        // then
        verify(refundService).reclaim(10L, Instant.ofEpochMilli(revokedAt), "APPLE/1", true);
    }

    @Test
    void handle_WhenRefundReversed_ThenCancelsTheRefund() throws Exception {
        // given
        stubPurchase(purchase(user(0, false), NOW));
        when(appStoreServerClient.fetchTransactionInfo(TRANSACTION_ID)).thenReturn(transaction(null, null));

        // when
        appleNotificationService.handle(notification("REFUND_REVERSED", BUNDLE_ID));

        // then
        verify(refundService).cancelRefund(10L);
        verify(refundService, never()).reclaim(anyLong(), any(), anyString(), anyBoolean());
    }

    @Test
    void handle_WhenRefundIsForgedButAppleShowsNoRevocation_ThenReclaimsNothing() throws Exception {
        // given
        stubPurchase(purchase(user(0, false), NOW));
        when(appStoreServerClient.fetchTransactionInfo(TRANSACTION_ID)).thenReturn(transaction(null, null));

        // when
        appleNotificationService.handle(notification("REFUND", BUNDLE_ID));

        // then
        verify(refundService, never()).reclaim(anyLong(), any(), anyString(), anyBoolean());
    }

    @Test
    void handle_WhenTransactionIsUnknown_ThenDoesNotAskApple() throws Exception {
        // given
        when(inAppPurchaseRepository.findByTransactionId(TRANSACTION_ID)).thenReturn(Optional.empty());

        // when
        appleNotificationService.handle(notification("REFUND", BUNDLE_ID));

        // then
        verifyNoInteractions(appStoreServerClient, refundService);
    }

    @Test
    void handle_WhenNotificationIsForAnotherApp_ThenIgnoresIt() throws Exception {
        // when
        appleNotificationService.handle(notification("REFUND", "com.other"));

        // then
        verifyNoInteractions(inAppPurchaseRepository, appStoreServerClient, refundService);
    }

    @Test
    void handle_WhenTest_ThenChangesNothing() throws Exception {
        // when
        appleNotificationService.handle(notification("TEST", BUNDLE_ID));

        // then
        verifyNoInteractions(inAppPurchaseRepository, appStoreServerClient, refundService);
    }

    @Test
    void handle_WhenPayloadIsMalformed_ThenThrowsInvalidPaymentRequest() {
        // when
        CustomException exception = assertThrows(CustomException.class,
                () -> appleNotificationService.handle("not-a-jws"));

        // then
        assertEquals(ErrorCode.INVALID_PAYMENT_REQUEST, exception.getErrorCode());
    }

    @Test
    void handle_WhenConsumptionRequestWithoutConsent_ThenSendsNothing() throws Exception {
        // given
        UserEntity user = user(1000, false);
        stubPurchase(purchase(user, NOW));
        when(appStoreServerClient.fetchTransactionInfo(TRANSACTION_ID)).thenReturn(transaction(null, null));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        appleNotificationService.handle(notification("CONSUMPTION_REQUEST", BUNDLE_ID));

        // then
        verify(appStoreServerClient, never()).sendConsumptionInformation(anyString(), anyString(), any());
    }

    @Test
    void handle_WhenConsumptionRequestForPartlySpentCurrency_ThenPrefersDecline() throws Exception {
        // given
        UserEntity user = user(250, true);
        stubPurchase(purchase(user, NOW.minus(Duration.ofDays(1))));
        when(appStoreServerClient.fetchTransactionInfo(TRANSACTION_ID)).thenReturn(transaction(null, null));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        appleNotificationService.handle(notification("CONSUMPTION_REQUEST", BUNDLE_ID));

        // then
        verify(appStoreServerClient).sendConsumptionInformation("Production", TRANSACTION_ID,
                new AppleConsumptionRequest(true, 75_000, AppleConsumptionRequest.DELIVERED,
                        AppleConsumptionRequest.DECLINE, false));
    }

    @Test
    void handle_WhenConsumptionRequestForUnusedCurrencyWithinPeriod_ThenPrefersFullRefund() throws Exception {
        // given
        UserEntity user = user(1000, true);
        stubPurchase(purchase(user, NOW.minus(Duration.ofDays(1))));
        when(appStoreServerClient.fetchTransactionInfo(TRANSACTION_ID)).thenReturn(transaction(null, null));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        appleNotificationService.handle(notification("CONSUMPTION_REQUEST", BUNDLE_ID));

        // then
        verify(appStoreServerClient).sendConsumptionInformation("Production", TRANSACTION_ID,
                new AppleConsumptionRequest(true, 0, AppleConsumptionRequest.DELIVERED,
                        AppleConsumptionRequest.GRANT_FULL, false));
    }

    private void stubPurchase(InAppPurchase purchase) {
        when(inAppPurchaseRepository.findByTransactionId(TRANSACTION_ID)).thenReturn(Optional.of(purchase));
    }

    private UserEntity user(int currency, boolean refundDataConsent) {
        UserEntity user = TestUserEntityBuilder.builder().withId(1L).withCurrency(currency).build();
        user.updateRefundDataConsent(refundDataConsent);
        return user;
    }

    private InAppPurchase purchase(UserEntity user, Instant createdAt) {
        return InAppPurchase.builder()
                .id(10L)
                .user(user)
                .platform(PaymentPlatform.IOS)
                .productId("piece_1000")
                .transactionId(TRANSACTION_ID)
                .status(InAppPurchaseStatus.VERIFIED)
                .grantedCurrency(1000)
                .createdAt(createdAt)
                .build();
    }

    private AppleTransactionInfo transaction(Long revocationDate, Integer revocationReason) {
        return new AppleTransactionInfo(TRANSACTION_ID, TRANSACTION_ID, BUNDLE_ID, "piece_1000", "Consumable",
                "Production", NOW.minusSeconds(3600).toEpochMilli(), 1, "PURCHASED", "PURCHASE",
                revocationDate, revocationReason);
    }

    private String notification(String type, String bundleId) throws Exception {
        String signedTransaction = fakeJws(objectMapper.writeValueAsString(Map.of(
                "transactionId", TRANSACTION_ID,
                "bundleId", bundleId,
                "productId", "piece_1000")));
        return fakeJws(objectMapper.writeValueAsString(Map.of(
                "notificationType", type,
                "notificationUUID", "uuid-1",
                "data", Map.of(
                        "bundleId", bundleId,
                        "environment", "Production",
                        "signedTransactionInfo", signedTransaction))));
    }

    // The service decodes only the payload part, so the header and signature can be placeholders
    private String fakeJws(String payloadJson) {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String header = encoder.encodeToString("{\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = encoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = encoder.encodeToString("fake-signature".getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + "." + signature;
    }
}
