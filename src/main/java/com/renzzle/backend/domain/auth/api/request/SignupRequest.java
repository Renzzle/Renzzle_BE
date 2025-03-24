package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotEmpty(message = "이메일 정보가 없습니다")
        @Email(message = "이메일 형식이 아닙니다")
        String email,

        @NotEmpty(message = "비밀번호 정보가 없습니다")
        @Pattern(regexp = "^(?=.*[A-Za-z])[A-Za-z\\d]{8,}$", message = "비밀번호의 형식이 올바르지 않습니다")
        String password,

        @NotEmpty(message = "닉네임 정보가 없습니다")
        @Pattern(regexp = "^[\\p{L}0-9]*$", message = "닉네임은 특수문자를 포함할 수 없습니다")
        @Size(min = 2, max = 8, message = "닉네임은 2글자 이상, 최대 8글자까지 가능합니다")
        String nickname,

        @NotEmpty(message = "토큰 정보가 없습니다")
        String authVerityToken,

        @NotEmpty(message = "기기 고유번호 정보가 없습니다")
        String deviceId
) { }
