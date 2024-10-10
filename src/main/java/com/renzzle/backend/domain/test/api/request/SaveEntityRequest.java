package com.renzzle.backend.domain.test.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveEntityRequest(
        @NotBlank(message = "이름 정보가 없습니다")
        @Size(max = 6, message = "이름은 6자 이하만 가능합니다")
        String name
) { }
