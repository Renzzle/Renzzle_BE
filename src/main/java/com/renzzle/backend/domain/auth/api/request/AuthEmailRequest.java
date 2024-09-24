package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthEmailRequest(
        @NotBlank(message = "이메일 정보가 없습니다")
        @Email(message = "이메일 형식이 아닙니다")
        String email
) { }
