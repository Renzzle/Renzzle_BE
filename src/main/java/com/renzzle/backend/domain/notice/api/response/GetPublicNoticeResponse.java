package com.renzzle.backend.domain.notice.api.response;

import lombok.Builder;

@Builder
public record GetPublicNoticeResponse(
        String title,
        String context,
        String createdAt,
        String expiredAt
) { }
