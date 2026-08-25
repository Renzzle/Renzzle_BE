package com.renzzle.backend.domain.payment.service;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppleTransactionVerifier {

    private final AppleAppStoreServerClient appStoreServerClient;
    private final String bundleId;

    public AppleTransactionVerifier(
            AppleAppStoreServerClient appStoreServerClient,
            @Value("${iap.apple.bundle-id}") String bundleId
    ) {
        this.appStoreServerClient = appStoreServerClient;
        this.bundleId = bundleId;
    }

    public StoreVerificationResult verify(String productId, String transactionId) {
        AppleTransactionInfo transaction = appStoreServerClient.fetchTransactionInfo(transactionId);

        if (!bundleId.equals(transaction.bundleId())) {
            log.warn("Apple transaction bundleId mismatch. expected={}, actual={}",
                    bundleId, transaction.bundleId());
            throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
        }

        if (transaction.revocationDate() != null) {
            log.warn("Apple transaction is revoked. transactionId={}, revocationReason={}",
                    transaction.transactionId(), transaction.revocationReason());
            throw new CustomException(ErrorCode.STORE_VERIFICATION_FAILED);
        }

        if (!transactionId.equals(transaction.transactionId())
                || !productId.equals(transaction.productId())) {
            log.warn("Apple transaction does not match request. requestedProductId={}, actualProductId={}, "
                            + "requestedTransactionId={}, actualTransactionId={}",
                    productId, transaction.productId(), transactionId, transaction.transactionId());
            throw new CustomException(ErrorCode.RECEIPT_TRANSACTION_MISMATCH);
        }

        log.info("Apple transaction verified. transactionId={}, productId={}, environment={}",
                transaction.transactionId(), transaction.productId(), transaction.environment());

        return new StoreVerificationResult(transaction.productId(), transaction.transactionId());
    }
}
