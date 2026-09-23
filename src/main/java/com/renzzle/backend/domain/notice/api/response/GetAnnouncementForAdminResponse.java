package com.renzzle.backend.domain.notice.api.response;

import lombok.Builder;

@Builder
public record GetAnnouncementForAdminResponse(
        long id,
        String langCode,
        String title,
        String context,
        String createdAt,
        String expiredAt,
        boolean isActive
) { }
