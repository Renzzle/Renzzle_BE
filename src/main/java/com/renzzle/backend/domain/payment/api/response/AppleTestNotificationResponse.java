package com.renzzle.backend.domain.payment.api.response;

public record AppleTestNotificationResponse(
        // Production or Sandbox: decides which registered URL Apple sends to
        String environment,
        // Checks the result for up to six months
        String testNotificationToken
) {
}
