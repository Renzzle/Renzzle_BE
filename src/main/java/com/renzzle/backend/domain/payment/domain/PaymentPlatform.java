package com.renzzle.backend.domain.payment.domain;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;

import java.util.Arrays;

public enum PaymentPlatform {
    ANDROID,
    IOS;

    public static PaymentPlatform from(String value) {
        return Arrays.stream(values())
                .filter(platform -> platform.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_PAYMENT_PLATFORM));
    }
}
