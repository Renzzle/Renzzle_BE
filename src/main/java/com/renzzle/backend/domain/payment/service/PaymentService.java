package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.domain.payment.api.request.VerifyInAppPurchaseRequest;
import com.renzzle.backend.domain.payment.api.response.VerifyInAppPurchaseResponse;
import com.renzzle.backend.domain.payment.dao.InAppPurchaseRepository;
import com.renzzle.backend.domain.payment.domain.IapProduct;
import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import com.renzzle.backend.domain.payment.domain.InAppPurchaseStatus;
import com.renzzle.backend.domain.payment.domain.PaymentPlatform;
import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final InAppPurchaseRepository inAppPurchaseRepository;
    private final UserRepository userRepository;
    private final GooglePlayReceiptVerifier googlePlayReceiptVerifier;
    private final AppleReceiptVerifier appleReceiptVerifier;

    @Transactional
    public VerifyInAppPurchaseResponse verifyInAppPurchase(UserEntity user, VerifyInAppPurchaseRequest request) {
        PaymentPlatform platform = PaymentPlatform.from(request.platform());

        return switch (platform) {
            case ANDROID -> verifyAndroidPurchase(user, request);
            case IOS -> verifyIosPurchase(user, request);
        };
    }

    private VerifyInAppPurchaseResponse verifyAndroidPurchase(UserEntity user, VerifyInAppPurchaseRequest request) {
        validateRequired(request.purchaseToken());
        if (inAppPurchaseRepository.existsByPurchaseToken(request.purchaseToken())) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED_RECEIPT);
        }

        StoreVerificationResult verificationResult =
                googlePlayReceiptVerifier.verify(request.productId(), request.purchaseToken());

        String transactionId = StringUtils.hasText(request.transactionId())
                ? request.transactionId()
                : verificationResult.transactionId();

        return grantReward(user, PaymentPlatform.ANDROID, request.productId(), transactionId,
                request.purchaseToken());
    }

    private VerifyInAppPurchaseResponse verifyIosPurchase(UserEntity user, VerifyInAppPurchaseRequest request) {
        validateRequired(request.receipt());
        validateRequired(request.transactionId());
        if (inAppPurchaseRepository.existsByTransactionId(request.transactionId())) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED_RECEIPT);
        }

        StoreVerificationResult verificationResult =
                appleReceiptVerifier.verify(request.productId(), request.transactionId(), request.receipt());

        if (!request.productId().equals(verificationResult.productId())
                || !request.transactionId().equals(verificationResult.transactionId())) {
            throw new CustomException(ErrorCode.RECEIPT_TRANSACTION_MISMATCH);
        }

        return grantReward(user, PaymentPlatform.IOS, request.productId(), request.transactionId(), null);
    }

    private VerifyInAppPurchaseResponse grantReward(
            UserEntity user,
            PaymentPlatform platform,
            String productId,
            String transactionId,
            String purchaseToken
    ) {
        IapProduct product = IapProduct.fromProductId(productId);
        UserEntity persistedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_USER));

        inAppPurchaseRepository.save(InAppPurchase.builder()
                .user(persistedUser)
                .platform(platform)
                .productId(productId)
                .transactionId(transactionId)
                .purchaseToken(purchaseToken)
                .status(InAppPurchaseStatus.VERIFIED)
                .grantedCurrency(product.getGrantedCurrency())
                .build());

        persistedUser.getReward(product.getGrantedCurrency());

        return VerifyInAppPurchaseResponse.builder()
                .platform(platform.name())
                .productId(productId)
                .grantedCurrency(product.getGrantedCurrency())
                .build();
    }

    private void validateRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new CustomException(ErrorCode.INVALID_PAYMENT_REQUEST);
        }
    }
}
