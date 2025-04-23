package com.renzzle.backend.domain.notice.api.response;

import lombok.Builder;

@Builder
public record GetPersonalNoticeResponse(
        String context
) { }
