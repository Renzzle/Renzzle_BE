package com.renzzle.backend.domain.payment.domain;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum IapProduct {
    PIECE_1000("piece_1000", "com.renzzle.piece1000", 1000, true),
    PIECE_5000("piece_5000", "com.renzzle.piece5000", 5000, true),
    PIECE_10000("piece_10000", "com.renzzle.piece10000", 10000, true),
    REMOVE_ADS("remove_ads", "com.renzzle.removeads", 0, false);

    // Google Play's ID, which purchases from both stores are saved under
    private final String productId;
    private final String appleProductId;
    private final int grantedCurrency;
    private final boolean grantsCurrency;

    IapProduct(String productId, String appleProductId, int grantedCurrency, boolean grantsCurrency) {
        this.productId = productId;
        this.appleProductId = appleProductId;
        this.grantedCurrency = grantedCurrency;
        this.grantsCurrency = grantsCurrency;
    }

    public boolean grantsCurrency() {
        return grantsCurrency;
    }

    public boolean removesAds() {
        return this == REMOVE_ADS;
    }

    // Takes either store's ID; the store verification already ties the ID to its platform
    public static IapProduct fromProductId(String productId) {
        return Arrays.stream(values())
                .filter(product -> product.productId.equals(productId)
                        || product.appleProductId.equals(productId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.UNKNOWN_IAP_PRODUCT));
    }
}
