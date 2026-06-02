package com.renzzle.backend.domain.payment.domain;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum IapProduct {
    PIECE_1000("piece_1000", 1000, true),
    PIECE_5000("piece_5000", 5000, true),
    PIECE_10000("piece_10000", 10000, true),
    REMOVE_ADS("remove_ads", 0, false);

    private final String productId;
    private final int grantedCurrency;
    private final boolean grantsCurrency;

    IapProduct(String productId, int grantedCurrency, boolean grantsCurrency) {
        this.productId = productId;
        this.grantedCurrency = grantedCurrency;
        this.grantsCurrency = grantsCurrency;
    }

    public boolean grantsCurrency() {
        return grantsCurrency;
    }

    public boolean removesAds() {
        return this == REMOVE_ADS;
    }

    public static IapProduct fromProductId(String productId) {
        return Arrays.stream(values())
                .filter(product -> product.productId.equals(productId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.UNKNOWN_IAP_PRODUCT));
    }
}
