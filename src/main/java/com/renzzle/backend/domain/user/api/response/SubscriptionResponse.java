package com.renzzle.backend.domain.user.api.response;

import com.renzzle.backend.domain.user.domain.Color;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionResponse {

    private Long userId;
    private String nickname;
    private Color profile;


}
