package com.renzzle.backend.domain.payment.domain;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum IapProduct {
    COIN_100("coin_100", 100),
    COIN_500("coin_500", 500),
    COIN_1000("coin_1000", 1000);

    private final String productId;
    private final int grantedCurrency;

    IapProduct(String productId, int grantedCurrency) {
        this.productId = productId;
        this.grantedCurrency = grantedCurrency;
    }

    public static IapProduct fromProductId(String productId) {
        return Arrays.stream(values())
                .filter(product -> product.productId.equals(productId))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.UNKNOWN_IAP_PRODUCT));
    }
}
