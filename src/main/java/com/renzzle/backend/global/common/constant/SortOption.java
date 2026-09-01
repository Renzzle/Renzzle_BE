package com.renzzle.backend.global.common.constant;

import java.util.Arrays;

public enum SortOption {

    LATEST, LIKE;

    public static SortOption from(String sort) {
        return Arrays.stream(values())
                .filter(option -> option.name().equalsIgnoreCase(sort))
                .findFirst()
                .orElse(LATEST);
    }

}
