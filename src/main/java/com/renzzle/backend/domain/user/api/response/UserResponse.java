package com.renzzle.backend.domain.user.api.response;
import lombok.Builder;

@Builder
public record UserResponse(
        Long id,
        String email,
        String nickname,
        int currency
) { }
