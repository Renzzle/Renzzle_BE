package com.renzzle.backend.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class TestTime {

    public static final Instant FIXED_INSTANT = Instant.parse("2025-01-01T00:00:00Z");
    public static final ZoneOffset FIXED_ZONE = ZoneOffset.UTC;
    public static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, FIXED_ZONE);

    private TestTime() {
    }
}
