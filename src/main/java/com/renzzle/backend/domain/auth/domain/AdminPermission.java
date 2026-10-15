package com.renzzle.backend.domain.auth.domain;

public enum AdminPermission {
    PACK_MANAGE,
    COMMUNITY_MANAGE,
    AI_RESPONSE_MANAGE,
    OPERATION_MANAGE;

    public String getAuthority() {
        return Admin.ADMIN_PREFIX + "_" + name();
    }
}
