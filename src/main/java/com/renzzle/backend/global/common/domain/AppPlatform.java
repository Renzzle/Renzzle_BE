package com.renzzle.backend.global.common.domain;

import java.util.Arrays;

public enum AppPlatform {
    ANDROID,
    IOS;

    public static AppPlatform from(String value) {
        return Arrays.stream(values())
                .filter(platform -> platform.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid platform: " + value));
    }
}
