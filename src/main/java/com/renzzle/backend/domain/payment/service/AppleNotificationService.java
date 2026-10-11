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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

// Handles App Store Server Notifications V2. Anyone can call the endpoint, so a notification is never trusted:
// it only names the transaction, which is fetched again from Apple before anything changes
@Slf4j
@Service
public class AppleNotificationService {

    static final String TEST = "TEST";
    static final String REFUND = "REFUND";
    static final String REFUND_REVERSED = "REFUND_REVERSED";
    static final String CONSUMPTION_REQUEST = "CONSUMPTION_REQUEST";

    private final AppleAppStoreServerClient appStoreServerClient;
    private final InAppPurchaseRepository inAppPurchaseRepository;
    private final UserRepository userRepository;
    private final RefundService refundService;
    private final RefundPolicy refundPolicy;
    private final ObjectMapper objectMapper;
    private final String bundleId;

    public AppleNotificationService(
            AppleAppStoreServerClient appStoreServerClient,
            InAppPurchaseRepository inAppPurchaseRepository,
            UserRepository userRepository,
            RefundService refundService,
            RefundPolicy refundPolicy,
            ObjectMapper objectMapper,
            @Value("${iap.apple.bundle-id}") String bundleId
    ) {
        this.appStoreServerClient = appStoreServerClient;
        this.inAppPurchaseRepository = inAppPurchaseRepository;
        this.userRepository = userRepository;
        this.refundService = refundService;
        this.refundPolicy = refundPolicy;
        this.objectMapper = objectMapper;
        this.bundleId = bundleId;
    }

    // Throwing makes the endpoint answer with an error, and Apple then sends the notification again
    public void handle(String signedPayload) {
        AppleNotificationPayload notification = decode(signedPayload, AppleNotificationPayload.class);
        String type = notification.notificationType();
        if (TEST.equals(type)) {
            log.info("Apple TEST notification received. notificationUUID={}", notification.notificationUUID());
            return;
        }
        if (!REFUND.equals(type) && !REFUND_REVERSED.equals(type) && !CONSUMPTION_REQUEST.equals(type)) {
            return;
        }

        AppleNotificationPayload.Data data = notification.data();
        if (data == null || !bundleId.equals(data.bundleId())) {
            log.warn("Apple notification for another app ignored. type={}, notificationUUID={}",
                    type, notification.notificationUUID());
            return;
        }

        String transactionId = decode(data.signedTransactionInfo(), AppleTransactionInfo.class).transactionId();
        Optional<InAppPurchase> purchase = inAppPurchaseRepository.findByTransactionId(transactionId)
                .filter(found -> found.getPlatform() == PaymentPlatform.IOS);
        if (purchase.isEmpty()) {
            // Never verified here, so nothing was granted
            log.info("Apple notification for an unknown transaction ignored. type={}, transactionId={}",
                    type, transactionId);
            return;
        }

        AppleTransactionInfo transaction = appStoreServerClient.fetchTransactionInfo(transactionId);
        if (!bundleId.equals(transaction.bundleId())) {
            log.warn("Apple transaction for another app ignored. transactionId={}", transactionId);
            return;
        }

        log.info("Apple notification received. type={}, notificationUUID={}, transactionId={}",
                type, notification.notificationUUID(), transactionId);
        if (CONSUMPTION_REQUEST.equals(type)) {
            sendConsumptionInformation(purchase.get(), transaction.environment());
        } else {
            applyRefundState(purchase.get(), transaction);
        }
    }

    // Follows what Apple reports now rather than the notification type, so a stale or forged one does no harm
    private void applyRefundState(InAppPurchase purchase, AppleTransactionInfo transaction) {
        if (transaction.revocationDate() != null) {
            refundService.reclaim(purchase.getId(), Instant.ofEpochMilli(transaction.revocationDate()),
                    "APPLE/" + transaction.revocationReason(), true);
        } else {
            refundService.cancelRefund(purchase.getId());
        }
    }

    // Apple weighs this when deciding a refund the customer asked it for. Sent only with the user's consent
    private void sendConsumptionInformation(InAppPurchase purchase, String environment) {
        if (purchase.getStatus() != InAppPurchaseStatus.VERIFIED) {
            return;
        }
        Optional<UserEntity> user = userRepository.findById(purchase.getUser().getId());
        if (user.isEmpty() || !user.get().isRefundDataConsent()) {
            log.info("Consumption information not sent without consent. purchaseId={}", purchase.getId());
            return;
        }

        AppleConsumptionRequest request = new AppleConsumptionRequest(
                true,
                refundPolicy.consumptionPercentage(purchase, user.get()),
                AppleConsumptionRequest.DELIVERED,
                refundPolicy.isRefundable(purchase, user.get())
                        ? AppleConsumptionRequest.GRANT_FULL
                        : AppleConsumptionRequest.DECLINE,
                false);
        appStoreServerClient.sendConsumptionInformation(environment, purchase.getTransactionId(), request);
        log.info("Consumption information sent to Apple. purchaseId={}, consumptionPercentage={}, refundPreference={}",
                purchase.getId(), request.consumptionPercentage(), request.refundPreference());
    }

    private <T> T decode(String jws, Class<T> type) {
        try {
            return AppleJws.decodePayload(jws, objectMapper, type);
        } catch (IllegalArgumentException e) {
            log.warn("Malformed Apple notification.", e);
            throw new CustomException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }
    }
}
