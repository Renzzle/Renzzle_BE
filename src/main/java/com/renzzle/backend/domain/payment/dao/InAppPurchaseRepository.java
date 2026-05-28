package com.renzzle.backend.domain.payment.dao;

import com.renzzle.backend.domain.payment.domain.InAppPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InAppPurchaseRepository extends JpaRepository<InAppPurchase, Long> {

    boolean existsByPurchaseToken(String purchaseToken);

    boolean existsByTransactionId(String transactionId);
}
