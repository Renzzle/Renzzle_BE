package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.dao.InAppPurchaseRepository;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.payment.domain.PaymentPlatform;
import com.renzzle.backend.support.TestUserEntityBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleVoidedPurchaseServiceTest {

    private static final Instant CUTOFF = Instant.parse("2026-10-20T00:00:00Z");
    private static final int REMORSE = 1;
    private static final int UNACKNOWLEDGED = GoogleVoidedPurchase.UNACKNOWLEDGED_PURCHASE;

    @Mock
    private GooglePlayClient googlePlayClient;

    @Mock
    private InAppPurchaseRepository inAppPurchaseRepository;

    @Mock
    private RefundService refundService;

    @Test
    void reclaimVoidedPurchases_WhenDisabled_ThenDoesNotCallGoogle() {
        // when
        service(false, "").reclaimVoidedPurchases();

        // then
        verifyNoInteractions(googlePlayClient, refundService);
    }

    @Test
    void reclaimVoidedPurchases_WhenOrderIdMatches_ThenReclaimsIt() {
        // given
        when(googlePlayClient.listVoidedPurchases(null))
                .thenReturn(page(null, voided("GPA.1", "token-1", REMORSE, CUTOFF)));
        when(inAppPurchaseRepository.findByTransactionId("GPA.1"))
                .thenReturn(Optional.of(purchase(10L, InAppPurchaseStatus.VERIFIED)));

        // when
        service(true, "").reclaimVoidedPurchases();

        // then
        verify(refundService).reclaim(10L, CUTOFF, "GOOGLE/1", true);
    }

    @Test
    void reclaimVoidedPurchases_WhenOrderIdUnknown_ThenMatchesByPurchaseToken() {
        // given
        when(googlePlayClient.listVoidedPurchases(null))
                .thenReturn(page(null, voided("GPA.1", "token-1", REMORSE, CUTOFF)));
        when(inAppPurchaseRepository.findByTransactionId("GPA.1")).thenReturn(Optional.empty());
        when(inAppPurchaseRepository.findFirstByPurchaseToken("token-1"))
                .thenReturn(Optional.of(purchase(10L, InAppPurchaseStatus.VERIFIED)));

        // when
        service(true, "").reclaimVoidedPurchases();

        // then
        verify(refundService).reclaim(10L, CUTOFF, "GOOGLE/1", true);
    }

    @Test
    void reclaimVoidedPurchases_WhenPurchaseWasNeverVerified_ThenSkipsIt() {
        // given
        when(googlePlayClient.listVoidedPurchases(null))
                .thenReturn(page(null, voided("GPA.1", "token-1", REMORSE, CUTOFF)));
        when(inAppPurchaseRepository.findByTransactionId("GPA.1")).thenReturn(Optional.empty());
        when(inAppPurchaseRepository.findFirstByPurchaseToken("token-1")).thenReturn(Optional.empty());

        // when
        service(true, "").reclaimVoidedPurchases();

        // then
        verifyNoInteractions(refundService);
    }

    @Test
    void reclaimVoidedPurchases_WhenAlreadyRefunded_ThenSkipsIt() {
        // given
        when(googlePlayClient.listVoidedPurchases(null))
                .thenReturn(page(null, voided("GPA.1", "token-1", REMORSE, CUTOFF)));
        when(inAppPurchaseRepository.findByTransactionId("GPA.1"))
                .thenReturn(Optional.of(purchase(10L, InAppPurchaseStatus.REFUNDED)));

        // when
        service(true, "").reclaimVoidedPurchases();

        // then
        verifyNoInteractions(refundService);
    }

    @Test
    void reclaimVoidedPurchases_WhenSeveralPages_ThenReadsEveryPage() {
        // given
        when(googlePlayClient.listVoidedPurchases(null))
                .thenReturn(page("page-2", voided("GPA.1", "token-1", REMORSE, CUTOFF)));
        when(googlePlayClient.listVoidedPurchases("page-2"))
                .thenReturn(page(null, voided("GPA.2", "token-2", REMORSE, CUTOFF)));
        when(inAppPurchaseRepository.findByTransactionId("GPA.1"))
                .thenReturn(Optional.of(purchase(10L, InAppPurchaseStatus.VERIFIED)));
        when(inAppPurchaseRepository.findByTransactionId("GPA.2"))
                .thenReturn(Optional.of(purchase(11L, InAppPurchaseStatus.VERIFIED)));

        // when
        service(true, "").reclaimVoidedPurchases();

        // then
        verify(refundService).reclaim(10L, CUTOFF, "GOOGLE/1", true);
        verify(refundService).reclaim(11L, CUTOFF, "GOOGLE/1", true);
    }

    @Test
    void reclaimVoidedPurchases_WhenOneFails_ThenStillAppliesTheRest() {
        // given
        when(googlePlayClient.listVoidedPurchases(null)).thenReturn(page(null,
                voided("GPA.1", "token-1", REMORSE, CUTOFF),
                voided("GPA.2", "token-2", REMORSE, CUTOFF)));
        when(inAppPurchaseRepository.findByTransactionId("GPA.1"))
                .thenReturn(Optional.of(purchase(10L, InAppPurchaseStatus.VERIFIED)));
        when(inAppPurchaseRepository.findByTransactionId("GPA.2"))
                .thenReturn(Optional.of(purchase(11L, InAppPurchaseStatus.VERIFIED)));
        when(refundService.reclaim(10L, CUTOFF, "GOOGLE/1", true))
                .thenThrow(new IllegalStateException("lock wait timeout"));

        // when
        service(true, "").reclaimVoidedPurchases();

        // then
        verify(refundService).reclaim(11L, CUTOFF, "GOOGLE/1", true);
    }

    @Test
    void shouldReclaimRewards_WhenOtherReason_ThenReclaimsWithoutCutoff() {
        // when
        boolean reclaim = service(true, "")
                .shouldReclaimRewards(voided("GPA.1", "token-1", REMORSE, CUTOFF.minusSeconds(60)));

        // then
        assertThat(reclaim).isTrue();
    }

    @Test
    void shouldReclaimRewards_WhenUnacknowledgedAndNoCutoff_ThenOnlyRecords() {
        // when
        boolean reclaim = service(true, "")
                .shouldReclaimRewards(voided("GPA.1", "token-1", UNACKNOWLEDGED, CUTOFF));

        // then
        assertThat(reclaim).isFalse();
    }

    @Test
    void shouldReclaimRewards_WhenUnacknowledgedBeforeCutoff_ThenOnlyRecords() {
        // when
        boolean reclaim = service(true, CUTOFF.toString())
                .shouldReclaimRewards(voided("GPA.1", "token-1", UNACKNOWLEDGED, CUTOFF.minusSeconds(1)));

        // then
        assertThat(reclaim).isFalse();
    }

    @Test
    void shouldReclaimRewards_WhenUnacknowledgedFromCutoff_ThenReclaims() {
        // when
        boolean reclaim = service(true, CUTOFF.toString())
                .shouldReclaimRewards(voided("GPA.1", "token-1", UNACKNOWLEDGED, CUTOFF));

        // then
        assertThat(reclaim).isTrue();
    }

    @Test
    void shouldReclaimRewards_WhenCutoffIsMalformed_ThenOnlyRecordsInsteadOfFailingStartup() {
        // when
        boolean reclaim = service(true, "2026-10-20")
                .shouldReclaimRewards(voided("GPA.1", "token-1", UNACKNOWLEDGED, CUTOFF));

        // then
        assertThat(reclaim).isFalse();
    }

    private GoogleVoidedPurchaseService service(boolean reclaimEnabled, String unacknowledgedReclaimSince) {
        return new GoogleVoidedPurchaseService(googlePlayClient, inAppPurchaseRepository, refundService,
                reclaimEnabled, unacknowledgedReclaimSince);
    }

    private GoogleVoidedPurchase voided(String orderId, String purchaseToken, int reason, Instant voidedAt) {
        return new GoogleVoidedPurchase(purchaseToken, orderId, voidedAt.toEpochMilli(), 0, reason);
    }

    private GoogleVoidedPurchasePage page(String nextPageToken, GoogleVoidedPurchase... voidedPurchases) {
        return new GoogleVoidedPurchasePage(
                List.of(voidedPurchases),
                nextPageToken == null ? null : new GoogleVoidedPurchasePage.TokenPagination(nextPageToken));
    }

    private InAppPurchase purchase(Long id, InAppPurchaseStatus status) {
        return InAppPurchase.builder()
                .id(id)
                .user(TestUserEntityBuilder.builder().withId(1L).build())
                .platform(PaymentPlatform.ANDROID)
                .productId("piece_1000")
                .status(status)
                .grantedCurrency(1000)
                .build();
    }
}
