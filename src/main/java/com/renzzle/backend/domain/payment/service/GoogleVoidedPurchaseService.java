package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.dao.InAppPurchaseRepository;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.payment.domain.PaymentPlatform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// Reflects refunds made on Google Play, found through its voided purchases list
@Slf4j
@Service
public class GoogleVoidedPurchaseService {

    // Stops a pagination loop; at 1,000 per page this is far more than 30 days of refunds
    private static final int MAX_PAGES = 50;

    private final GooglePlayClient googlePlayClient;
    private final InAppPurchaseRepository inAppPurchaseRepository;
    private final RefundService refundService;
    private final boolean reclaimEnabled;
    private final Instant unacknowledgedReclaimSince;

    public GoogleVoidedPurchaseService(
            GooglePlayClient googlePlayClient,
            InAppPurchaseRepository inAppPurchaseRepository,
            RefundService refundService,
            @Value("${iap.refund.google.reclaim-enabled}") boolean reclaimEnabled,
            @Value("${iap.refund.google.unacknowledged-reclaim-since}") String unacknowledgedReclaimSince
    ) {
        this.googlePlayClient = googlePlayClient;
        this.inAppPurchaseRepository = inAppPurchaseRepository;
        this.refundService = refundService;
        this.reclaimEnabled = reclaimEnabled;
        this.unacknowledgedReclaimSince = parseInstantOrNull(unacknowledgedReclaimSince);
    }

    // A malformed value must not keep the app from starting, so it falls back to never reclaiming reason 8
    private static Instant parseInstantOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException e) {
            log.error("Ignoring malformed iap.refund.google.unacknowledged-reclaim-since: {}", value);
            return null;
        }
    }

    // Reads the whole 30 days each time: refunds already applied are skipped
    @Scheduled(initialDelay = 5, fixedDelay = 60, timeUnit = TimeUnit.MINUTES)
    public void reclaimVoidedPurchases() {
        if (!reclaimEnabled) {
            return;
        }

        List<GoogleVoidedPurchase> voidedPurchases;
        try {
            voidedPurchases = fetchVoidedPurchases();
        } catch (Exception e) {
            log.warn("Failed to list Google Play voided purchases.", e);
            return;
        }

        int refundedCount = 0;
        for (GoogleVoidedPurchase voidedPurchase : voidedPurchases) {
            try {
                if (reclaim(voidedPurchase)) {
                    refundedCount++;
                }
            } catch (Exception e) {
                // The next run retries it
                log.warn("Failed to apply a Google Play refund. orderId={}", voidedPurchase.orderId(), e);
            }
        }
        if (refundedCount > 0) {
            log.info("Applied Google Play refunds. refundedCount={}, voidedCount={}",
                    refundedCount, voidedPurchases.size());
        }
    }

    public List<GoogleVoidedPurchase> fetchVoidedPurchases() {
        List<GoogleVoidedPurchase> voidedPurchases = new ArrayList<>();
        String pageToken = null;
        for (int page = 0; page < MAX_PAGES; page++) {
            GoogleVoidedPurchasePage response = googlePlayClient.listVoidedPurchases(pageToken);
            if (response == null) {
                break;
            }
            voidedPurchases.addAll(response.voidedPurchasesOrEmpty());
            pageToken = response.nextPageToken();
            if (!StringUtils.hasText(pageToken)) {
                break;
            }
        }
        return voidedPurchases;
    }

    // The order ID is stored as the transaction ID, unless Google gave none at verification and the token was used
    public Optional<InAppPurchase> findPurchase(GoogleVoidedPurchase voidedPurchase) {
        Optional<InAppPurchase> purchase = Optional.empty();
        if (StringUtils.hasText(voidedPurchase.orderId())) {
            purchase = inAppPurchaseRepository.findByTransactionId(voidedPurchase.orderId());
        }
        if (purchase.isEmpty() && StringUtils.hasText(voidedPurchase.purchaseToken())) {
            purchase = inAppPurchaseRepository.findFirstByPurchaseToken(voidedPurchase.purchaseToken());
        }
        return purchase.filter(found -> found.getPlatform() == PaymentPlatform.ANDROID);
    }

    // Refunds caused by the app never acknowledging the purchase were our fault, so before the configured time
    // (or always, when it is unset) they are only recorded
    public boolean shouldReclaimRewards(GoogleVoidedPurchase voidedPurchase) {
        if (!voidedPurchase.isUnacknowledgedPurchase()) {
            return true;
        }
        Instant voidedAt = voidedPurchase.voidedAt();
        return unacknowledgedReclaimSince != null && voidedAt != null && !voidedAt.isBefore(unacknowledgedReclaimSince);
    }

    private boolean reclaim(GoogleVoidedPurchase voidedPurchase) {
        Optional<InAppPurchase> purchase = findPurchase(voidedPurchase);
        // Never verified means nothing was granted; already refunded is checked again under lock in RefundService
        if (purchase.isEmpty() || purchase.get().getStatus() != InAppPurchaseStatus.VERIFIED) {
            return false;
        }

        return refundService.reclaim(
                purchase.get().getId(),
                voidedPurchase.voidedAt(),
                "GOOGLE/" + voidedPurchase.voidedReason(),
                shouldReclaimRewards(voidedPurchase));
    }
}
