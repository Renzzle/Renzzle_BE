package com.renzzle.backend.domain.user.api.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SubscriptionResponse {

    private Long userId;
    private String nickname;
    private String profile;


}
