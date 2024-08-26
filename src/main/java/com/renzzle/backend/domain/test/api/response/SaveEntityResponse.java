package com.renzzle.backend.domain.test.api.response;

import lombok.Builder;

@Builder
public record SaveEntityResponse(Long id, String name) {
}
