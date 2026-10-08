package com.renzzle.backend.global.common.constant;

import lombok.Getter;

// Live values come from app_info by tag; the default seeds it and covers a missing row
@Getter
public enum ItemPrice {

    CHANGE_NICKNAME("change_nickname_price", 3000),
    HINT("hint_price", 200),
    ATTENDANCE_REWARD("attendance_reward", 200),
    RANK_REWARD("rank_reward", 10),
    TRAINING_REWARD("training_reward", 10),
    COMMUNITY_REWARD("community_reward", 10);

    private final String tag;
    private final int defaultPrice;

    ItemPrice(String tag, int defaultPrice) {
        this.tag = tag;
        this.defaultPrice = defaultPrice;
    }
}
