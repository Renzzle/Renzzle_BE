package com.renzzle.backend.domain.notice.api.request;

import com.renzzle.backend.domain.notice.util.AppVersionUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSystemInfoRequest(
        @NotBlank(message = "Android version is required")
        @Size(max = 255, message = "Android version must be 255 characters or fewer")
        @Pattern(regexp = AppVersionUtil.VERSION_REGEX, message = "Invalid Android version format")
        String androidVersion,

        @NotBlank(message = "iOS version is required")
        @Size(max = 255, message = "iOS version must be 255 characters or fewer")
        @Pattern(regexp = AppVersionUtil.VERSION_REGEX, message = "Invalid iOS version format")
        String iosVersion,

        @NotNull(message = "System check flag is required")
        Boolean isSystemCheck
) { }
