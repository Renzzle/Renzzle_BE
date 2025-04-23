package com.renzzle.backend.domain.notice.api.response;

import lombok.Builder;

@Builder
public record GetVersionCodeResponse(
        String version
) { }
