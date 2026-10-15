package com.renzzle.backend.domain.admin.api.request;

import com.renzzle.backend.domain.auth.domain.AdminPermission;
import com.renzzle.backend.global.util.EmailUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AddAdminAccountRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotEmpty(message = "At least one permission is required")
        Set<@NotNull(message = "Permission must not be null") AdminPermission> permissions
) {
    public AddAdminAccountRequest {
        email = EmailUtils.normalize(email);
    }
}
