package com.renzzle.backend.domain.notice.api.response;

import lombok.Builder;

@Builder
public record GetSystemInfoForAdminResponse(
        String androidVersion,
        String iosVersion,
        boolean isSystemCheck
) { }
