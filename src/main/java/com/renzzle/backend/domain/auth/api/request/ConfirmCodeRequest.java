package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record ConfirmCodeRequest(
        @NotEmpty(message = "이메일 정보가 없습니다")
        @Email(message = "이메일 형식이 아닙니다")
        String email,

        @NotEmpty(message = "인증 코드 정보가 없습니다")
        @Pattern(regexp = "^\\d{6}$", message = "올바른 코드 형식이 아닙니다")
        String code
) { }
