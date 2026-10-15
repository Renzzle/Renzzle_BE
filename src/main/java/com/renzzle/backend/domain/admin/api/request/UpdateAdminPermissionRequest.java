package com.renzzle.backend.domain.admin.api.request;

import com.renzzle.backend.domain.auth.domain.AdminPermission;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdateAdminPermissionRequest(
        @NotEmpty(message = "At least one permission is required")
        Set<@NotNull(message = "Permission must not be null") AdminPermission> permissions
) { }
