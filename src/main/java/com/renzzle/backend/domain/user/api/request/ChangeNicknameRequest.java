package com.renzzle.backend.domain.user.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangeNicknameRequest(
        @NotEmpty(message = "닉네임 정보가 없습니다")
        @Pattern(regexp = "^[\\p{L}0-9]*$", message = "닉네임은 특수문자를 포함할 수 없습니다")
        @Size(min = 2, max = 8, message = "닉네임은 2글자 이상, 최대 8글자까지 가능합니다")
        String nickname
) { }
