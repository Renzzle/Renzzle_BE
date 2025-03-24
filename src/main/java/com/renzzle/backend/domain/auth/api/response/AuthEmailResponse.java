package com.renzzle.backend.domain.auth.api.response;

import lombok.Builder;

@Builder
public record AuthEmailResponse(
        int requestCount
) { }
