package com.renzzle.backend.domain.test.api.response;

import lombok.Builder;

@Builder
public record HelloResponse(
        String message
) { }
