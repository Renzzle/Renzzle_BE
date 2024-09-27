package com.renzzle.backend.domain.user.api.response;

import com.renzzle.backend.domain.user.domain.Color;
import com.renzzle.backend.domain.user.domain.UserLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String nickname;
    private UserLevel level;
    private Color profile;
}
