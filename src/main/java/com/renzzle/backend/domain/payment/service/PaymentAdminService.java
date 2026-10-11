package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.api.response.AppleTestNotificationResponse;
import com.renzzle.backend.domain.payment.api.response.GetAppleTestNotificationStatusResponse;
import com.renzzle.backend.domain.payment.api.response.GetGoogleVoidedPurchaseForAdminResponse;
import com.renzzle.backend.domain.payment.api.response.GetPurchaseForAdminResponse;
import com.renzzle.backend.domain.payment.dao.InAppPurchaseRepository;
import com.renzzle.backend.domain.payment.domain.IapProduct;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Lookups for the admin payments page, which CS uses to judge refund requests
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentAdminService {

    private final InAppPurchaseRepository inAppPurchaseRepository;
    private final UserRepository userRepository;
    private final RefundPolicy refundPolicy;
    private final GoogleVoidedPurchaseService googleVoidedPurchaseService;
    private final AppleAppStoreServerClient appStoreServerClient;

    // By Google order ID or App Store transaction ID, purchase token, or a user's exact email or nickname
    @Transactional(readOnly = true)
    public List<GetPurchaseForAdminResponse> searchPurchasesForAdmin(String keyword) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            throw new CustomException("Keyword is required", ErrorCode.VALIDATION_ERROR);
        }

        List<InAppPurchase> purchases = inAppPurchaseRepository.findByTransactionId(trimmed)
                .or(() -> inAppPurchaseRepository.findFirstByPurchaseToken(trimmed))
                .map(List::of)
                .orElseGet(() -> findUserPurchases(trimmed));
        return purchases.stream()
                .map(this::toPurchaseResponse)
                .toList();
    }

    // Asks Google live, newest refund first
    @Transactional(readOnly = true)
    public List<GetGoogleVoidedPurchaseForAdminResponse> getGoogleVoidedPurchasesForAdmin() {
        List<GoogleVoidedPurchase> voidedPurchases;
        try {
            voidedPurchases = googleVoidedPurchaseService.fetchVoidedPurchases();
        } catch (RuntimeException e) {
            log.warn("Failed to list Google Play voided purchases for admin.", e);
            throw new CustomException(ErrorCode.STORE_API_FAILED);
        }

        return voidedPurchases.stream()
                .sorted(Comparator.comparing(GoogleVoidedPurchase::voidedTimeMillis,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toVoidedPurchaseResponse)
                .toList();
    }

    // Asks Apple to send a TEST notification, which shows whether it can reach the notification URL
    public AppleTestNotificationResponse requestAppleTestNotificationForAdmin(String environment) {
        String appleEnvironment = toAppleEnvironment(environment);
        try {
            return new AppleTestNotificationResponse(appleEnvironment,
                    appStoreServerClient.requestTestNotification(appleEnvironment));
        } catch (RuntimeException e) {
            log.warn("Failed to request an Apple test notification. environment={}", appleEnvironment, e);
            throw new CustomException(ErrorCode.STORE_API_FAILED);
        }
    }

    public GetAppleTestNotificationStatusResponse getAppleTestNotificationStatusForAdmin(
            String environment, String testNotificationToken) {
        AppleTestNotificationStatus status;
        try {
            status = appStoreServerClient.getTestNotificationStatus(
                    toAppleEnvironment(environment), testNotificationToken);
        } catch (RuntimeException e) {
            log.warn("Failed to read an Apple test notification status.", e);
            throw new CustomException(ErrorCode.STORE_API_FAILED);
        }

        List<AppleTestNotificationStatus.SendAttempt> attempts =
                status == null || status.sendAttempts() == null ? List.of() : status.sendAttempts();
        return new GetAppleTestNotificationStatusResponse(attempts.stream()
                .map(attempt -> new GetAppleTestNotificationStatusResponse.SendAttempt(
                        attempt.attemptDate() == null ? null : Instant.ofEpochMilli(attempt.attemptDate()),
                        attempt.sendAttemptResult()))
                .toList());
    }

    private String toAppleEnvironment(String environment) {
        return "sandbox".equalsIgnoreCase(environment) ? "Sandbox" : "Production";
    }

    private List<InAppPurchase> findUserPurchases(String keyword) {
        return userRepository.findByEmail(keyword)
                .or(() -> userRepository.findByNickname(keyword))
                .map(user -> inAppPurchaseRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()))
                .orElse(List.of());
    }

    private GetPurchaseForAdminResponse toPurchaseResponse(InAppPurchase purchase) {
        UserEntity user = purchase.getUser();
        return GetPurchaseForAdminResponse.builder()
                .purchaseId(purchase.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .platform(purchase.getPlatform().name())
                .productId(purchase.getProductId())
                .transactionId(purchase.getTransactionId())
                .status(purchase.getStatus().name())
                .grantedCurrency(purchase.getGrantedCurrency())
                .currentCurrency(user.getCurrency())
                .purchasedAt(purchase.getCreatedAt())
                .withdrawalDeadline(refundPolicy.withdrawalDeadline(purchase))
                .withinWithdrawalPeriod(refundPolicy.isWithinWithdrawalPeriod(purchase))
                .unused(refundPolicy.isUnused(purchase, user))
                .refundable(refundPolicy.isRefundable(purchase, user))
                .refundedAt(purchase.getRefundedAt())
                .refundReason(purchase.getRefundReason())
                .reclaimedCurrency(purchase.getReclaimedCurrency())
                .build();
    }

    private GetGoogleVoidedPurchaseForAdminResponse toVoidedPurchaseResponse(GoogleVoidedPurchase voidedPurchase) {
        boolean reclaimRewards = googleVoidedPurchaseService.shouldReclaimRewards(voidedPurchase);
        var builder = GetGoogleVoidedPurchaseForAdminResponse.builder()
                .orderId(voidedPurchase.orderId())
                .voidedAt(voidedPurchase.voidedAt())
                .voidedSource(voidedPurchase.voidedSource())
                .voidedReason(voidedPurchase.voidedReason())
                .reclaimRewards(reclaimRewards);

        Optional<InAppPurchase> found = googleVoidedPurchaseService.findPurchase(voidedPurchase);
        if (found.isEmpty()) {
            return builder.matched(false).build();
        }

        InAppPurchase purchase = found.get();
        UserEntity user = purchase.getUser();
        return builder
                .matched(true)
                .purchaseId(purchase.getId())
                .userId(user.getId())
                .nickname(user.getNickname())
                .productId(purchase.getProductId())
                .status(purchase.getStatus().name())
                .grantedCurrency(purchase.getGrantedCurrency())
                .currentCurrency(user.getCurrency())
                .reclaimCurrency(reclaimCurrency(purchase, user, reclaimRewards))
                .build();
    }

    private int reclaimCurrency(InAppPurchase purchase, UserEntity user, boolean reclaimRewards) {
        if (purchase.getStatus() == InAppPurchaseStatus.REFUNDED) {
            return purchase.getReclaimedCurrency();
        }
        if (purchase.getStatus() != InAppPurchaseStatus.VERIFIED || !reclaimRewards
                || !IapProduct.fromProductId(purchase.getProductId()).grantsCurrency()) {
            return 0;
        }
        return user.reclaimableCurrency(purchase.getGrantedCurrency());
    }
}
