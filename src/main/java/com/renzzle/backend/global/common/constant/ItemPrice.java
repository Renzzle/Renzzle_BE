package com.renzzle.backend.global.common.constant;

import lombok.Getter;

@Getter
public enum ItemPrice {

    CHANGE_NICKNAME(3000),
    HINT(200),
    RANK_REWARD(10),
    TRAINING_LOW_REWARD(10),
    TRAINING_MIDDLE_REWARD(30),
    TRAINING_HIGH_REWARD(50);

    private final int price;

    ItemPrice(int price) {
        this.price = price;
    }
}