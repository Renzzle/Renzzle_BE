package com.renzzle.backend.global.common.constant;

import lombok.Getter;

@Getter
public enum ItemPrice {

    CHANGE_NICKNAME(3000),
    HINT(200),
    RANK_REWARD(10),
    TRAINING_REWARD(10),
    COMMUNITY_REWARD(10);

    private final int price;

    ItemPrice(int price) {
        this.price = price;
    }
}