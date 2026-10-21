package com.renzzle.backend.domain.payment.service;

// App Store Server API ConsumptionRequest for Send Consumption Information (v2)
public record AppleConsumptionRequest(
        // Apple rejects anything but true; without consent no request is sent at all
        boolean customerConsented,
        // In thousandths of a percent, 0 to 100000
        int consumptionPercentage,
        String deliveryStatus,
        String refundPreference,
        boolean sampleContentProvided
) {

    public static final String DELIVERED = "DELIVERED";
    public static final String GRANT_FULL = "GRANT_FULL";
    public static final String DECLINE = "DECLINE";
}
