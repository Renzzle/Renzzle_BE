package com.renzzle.backend.domain.test.api.response;

import lombok.Builder;

@Builder
public record FindEntityResponse(Long id, String name) {
}
