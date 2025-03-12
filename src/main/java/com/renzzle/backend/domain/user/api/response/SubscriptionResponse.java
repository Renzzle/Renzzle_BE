package com.renzzle.backend.domain.user.api.response;

import lombok.Builder;

@Builder
public record SubscriptionResponse(
        Long userId,
        String nickname
) {
}
