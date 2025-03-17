package com.renzzle.backend.domain.user.api.response;

import lombok.Builder;

@Builder
public record ChangeNicknameResponse(
        int currency
) { }
