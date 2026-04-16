package com.renzzle.backend.domain.appinfo.api.response;

import lombok.Builder;

@Builder
public record GetAppInfoResponse(
        String tag,
        String value
) { }
