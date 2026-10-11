package com.renzzle.backend.domain.payment.api.request;

import jakarta.validation.constraints.NotBlank;

// App Store Server Notifications V2 request body
public record AppleNotificationRequest(
        @NotBlank(message = "signedPayload is required")
        String signedPayload
) {
}
