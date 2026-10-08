package com.renzzle.backend.domain.notice.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppVersionUtilTest {

    @Test
    void compare_WhenSameVersion_ThenReturnsZero() {
        assertThat(AppVersionUtil.compare("1.2.3.4", "1.2.3.4")).isZero();
    }

    @Test
    void compare_WhenEarlierSegmentDiffers_ThenItDecidesRegardlessOfLaterSegments() {
        assertThat(AppVersionUtil.compare("1.9.9", "2.0.0")).isNegative();
        assertThat(AppVersionUtil.compare("2.0.0", "1.9.9")).isPositive();
    }

    @Test
    void compare_WhenSegmentHasMoreDigits_ThenComparesNumericallyNotLexically() {
        assertThat(AppVersionUtil.compare("1.0.10", "1.0.9")).isPositive();
        assertThat(AppVersionUtil.compare("1.0.9", "1.0.10")).isNegative();
    }

    @Test
    void compare_WhenSegmentCountDiffers_ThenMissingSegmentsCountAsZero() {
        assertThat(AppVersionUtil.compare("1.2", "1.2.0.0")).isZero();
        assertThat(AppVersionUtil.compare("1.2", "1.2.0.1")).isNegative();
        assertThat(AppVersionUtil.compare("1.2.0.1", "1.2")).isPositive();
    }

    @Test
    void compare_WhenLeadingZerosOrSurroundingWhitespace_ThenIgnoresThem() {
        assertThat(AppVersionUtil.compare(" 1.02.003 ", "1.2.3")).isZero();
    }

    @Test
    void compare_WhenSegmentExceedsLongRange_ThenStillCompares() {
        assertThat(AppVersionUtil.compare("1.99999999999999999999", "1.99999999999999999998")).isPositive();
    }

    @Test
    void versionRegex_WhenDotSeparatedNumbers_ThenMatches() {
        assertThat("1").matches(AppVersionUtil.VERSION_REGEX);
        assertThat("1.0.0").matches(AppVersionUtil.VERSION_REGEX);
        assertThat(" 1.2.3.4 ").matches(AppVersionUtil.VERSION_REGEX);
    }

    @Test
    void versionRegex_WhenMalformed_ThenDoesNotMatch() {
        assertThat("v1.0.0").doesNotMatch(AppVersionUtil.VERSION_REGEX);
        assertThat("1.0.0-beta").doesNotMatch(AppVersionUtil.VERSION_REGEX);
        assertThat("1..0").doesNotMatch(AppVersionUtil.VERSION_REGEX);
        assertThat("1.0.").doesNotMatch(AppVersionUtil.VERSION_REGEX);
        assertThat(".1.0").doesNotMatch(AppVersionUtil.VERSION_REGEX);
    }

}
