package com.renzzle.backend.domain.notice.api.response;

import lombok.Builder;

@Builder
public record GetNoticeRecipientResponse(
        long id,
        String email,
        String nickname
) { }
