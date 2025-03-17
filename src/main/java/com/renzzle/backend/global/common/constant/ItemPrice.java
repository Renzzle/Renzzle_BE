package com.renzzle.backend.global.common.constant;

import lombok.Getter;

@Getter
public enum ItemPrice {

    CHANGE_NICKNAME(2500),
    HINT(100),
    PUZZLE_PACK_1000(1000),
    PUZZLE_PACK_2000(2000),
    PUZZLE_PACK_3000(3000),
    PUZZLE_PACK_4000(4000),
    PUZZLE_PACK_5000(5000),
    PUZZLE_PACK_6000(6000);

    private final int price;

    ItemPrice(int price) {
        this.price = price;
    }

}
