package com.renzzle.backend.domain.notice.api.response;

import lombok.Builder;

@Builder
public record GetSystemInfoForAdminResponse(
        String version,
        boolean isSystemCheck
) { }
