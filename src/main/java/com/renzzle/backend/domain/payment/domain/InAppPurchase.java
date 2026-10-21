package com.renzzle.backend.domain.payment.domain;

import com.renzzle.backend.domain.user.domain.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "in_app_purchase",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transaction_id", columnNames = "transaction_id")
        },
        indexes = {
                @Index(name = "idx_purchase_token", columnList = "purchase_token")
        }
)
public class InAppPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    private PaymentPlatform platform;

    @Column(name = "product_id", nullable = false, length = 100)
    private String productId;

    @Column(name = "transaction_id", length = 512)
    private String transactionId;

    @Column(name = "purchase_token", length = 512)
    private String purchaseToken;

    @Convert(converter = InAppPurchaseStatusConverter.class)
    @Column(name = "status", nullable = false, length = 20)
    private InAppPurchaseStatus status;

    @Column(name = "granted_currency", nullable = false)
    private int grantedCurrency;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    // Store and its reason code, e.g. GOOGLE/8 or APPLE/0
    @Column(name = "refund_reason", length = 30)
    private String refundReason;

    // Less than grantedCurrency when the user had already spent part of it
    @Column(name = "reclaimed_currency", nullable = false)
    private int reclaimedCurrency;

    public void markRefunded(Instant refundedAt, String refundReason, int reclaimedCurrency) {
        this.status = InAppPurchaseStatus.REFUNDED;
        this.refundedAt = refundedAt;
        this.refundReason = refundReason;
        this.reclaimedCurrency = reclaimedCurrency;
    }

    public void cancelRefund() {
        this.status = InAppPurchaseStatus.VERIFIED;
        this.refundedAt = null;
        this.refundReason = null;
        this.reclaimedCurrency = 0;
    }
}
