package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record ConfirmCodeRequest(
        @Email(message = "이메일 형식이 아닙니다")
        String email,
        @Pattern(regexp = "^\\d{6}$", message = "올바른 코드 형식이 아닙니다")
        String code
) { }
