package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// App Store Server API CheckTestNotificationResponse: one attempt plus up to five retries
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppleTestNotificationStatus(
        List<SendAttempt> sendAttempts
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SendAttempt(
            Long attemptDate,
            // SUCCESS, or what went wrong, e.g. TIMED_OUT or UNSUCCESSFUL_HTTP_RESPONSE_CODE
            String sendAttemptResult
    ) {
    }
}
