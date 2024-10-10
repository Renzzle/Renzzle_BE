package com.renzzle.backend.domain.user.api.response;

public record UserResponse(
        Long id,
        String email,
        String nickname,
        String level,
        String profile
) {
}
