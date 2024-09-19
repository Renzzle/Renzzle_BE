package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Email(message = "이메일 형식이 아닙니다")
        String email,
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", message = "비밀번호의 형식이 올바르지 않습니다")
        String password,
        @Size(min = 2, max = 8, message = "닉네임은 2글자 이상, 최대 8글자까지 가능합니다")
        String nickname,
        String authVerityToken
) { }
