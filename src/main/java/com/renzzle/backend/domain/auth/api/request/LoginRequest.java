package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotEmpty(message = "이메일 정보가 없습니다")
        @Email(message = "이메일 형식이 아닙니다")
        String email,
        @NotEmpty(message = "비밀번호 정보가 없습니다")
        @Pattern(regexp = "^(?=.*[A-Za-z])[A-Za-z\\d]{8,}$", message = "비밀번호의 형식이 올바르지 않습니다")
        String password
) { }
