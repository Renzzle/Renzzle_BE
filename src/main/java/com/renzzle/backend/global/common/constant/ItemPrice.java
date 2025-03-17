package com.renzzle.backend.global.common.constant;

import lombok.Getter;

@Getter
public enum ItemPrice {

    CHANGE_NICKNAME(-2500),
    HINT(-100);

    private final int price;

    ItemPrice(int price) {
        this.price = price;
    }

}
