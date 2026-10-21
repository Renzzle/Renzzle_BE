package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// App Store Server Notifications V2 responseBodyV2DecodedPayload
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppleNotificationPayload(
        String notificationType,
        String subtype,
        String notificationUUID,
        Data data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String bundleId,
            // Production or Sandbox
            String environment,
            String signedTransactionInfo,
            // Only on CONSUMPTION_REQUEST
            String consumptionRequestReason
    ) {
    }
}
