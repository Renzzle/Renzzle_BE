package com.renzzle.backend.domain.payment.api.response;

import java.time.Instant;
import java.util.List;

// Empty attempts means Apple has not tried yet
public record GetAppleTestNotificationStatusResponse(
        List<SendAttempt> sendAttempts
) {

    public record SendAttempt(
            Instant attemptedAt,
            // SUCCESS, or why Apple could not deliver it
            String result
    ) {
    }
}
