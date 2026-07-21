package com.renzzle.backend.domain.auth.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotEmpty(message = "현재 비밀번호 정보가 없습니다")
        String currentPassword,

        @NotEmpty(message = "새 비밀번호 정보가 없습니다")
        @Pattern(regexp = "^(?=.*[A-Za-z])[A-Za-z\\d]{8,}$", message = "새 비밀번호의 형식이 올바르지 않습니다")
        String newPassword
) { }
