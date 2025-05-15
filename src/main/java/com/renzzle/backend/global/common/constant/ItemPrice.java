package com.renzzle.backend.global.common.constant;

import lombok.Getter;

@Getter
public enum ItemPrice {

    CHANGE_NICKNAME(2500),
    HINT(100),
    RANK_REWARD(20),
    TRAINING_LOW_REWARD(20),
    TRAINING_MIDDLE_REWARD(40),
    TRAINING_HIGH_REWARD(60);

    private final int price;

    ItemPrice(int price) {
        this.price = price;
    }
}