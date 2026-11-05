package com.renzzle.backend.domain.user.api.response;
import com.renzzle.backend.domain.auth.domain.AdminPermission;
import lombok.Builder;

import java.util.List;

@Builder
public record UserResponse(
        Long id,
        String email,
        String nickname,
        int currency,
        boolean adsRemoved,
        boolean refundDataConsent,
        // Empty for anyone who isn't an admin; the app shows the admin menus these allow
        List<AdminPermission> adminPermissions
) { }
