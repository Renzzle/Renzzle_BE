package com.renzzle.backend.domain.admin.api.response;

import com.renzzle.backend.domain.auth.domain.AdminPermission;

import java.util.List;

public record GetAdminAccountResponse(
        Long adminId,
        String email,
        String nickname,
        List<AdminPermission> permissions,
        boolean isMe
) { }
