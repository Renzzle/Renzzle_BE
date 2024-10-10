package com.renzzle.backend.domain.user.api.response;

import com.renzzle.backend.domain.user.domain.Color;

public record SubscriptionResponse(
        Long userId,
        String nickname,
        Color profile
) {
}
