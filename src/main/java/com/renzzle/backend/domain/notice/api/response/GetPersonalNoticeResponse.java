package com.renzzle.backend.domain.notice.api.response;

import lombok.Builder;

import java.util.List;

@Builder
public record GetPersonalNoticeResponse(
        String description,
        List<NoticeContext> notice,
        String version
) { }
