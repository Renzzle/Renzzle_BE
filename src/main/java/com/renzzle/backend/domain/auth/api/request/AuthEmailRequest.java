package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.Email;

public record AuthEmailRequest(
        @Email(message = "이메일 형식이 아닙니다")
        String email
) { }
