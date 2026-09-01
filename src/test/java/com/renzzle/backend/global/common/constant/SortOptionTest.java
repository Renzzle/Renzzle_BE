package com.renzzle.backend.global.common.constant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SortOptionTest {

    @Test
    void from_WhenSortIsLowerCase_ThenResolvesToEnumConstant() {
        // @ValidEnum accepts input case-insensitively, so the lookup must too
        assertThat(SortOption.from("latest")).isEqualTo(SortOption.LATEST);
        assertThat(SortOption.from("like")).isEqualTo(SortOption.LIKE);
        assertThat(SortOption.from("Like")).isEqualTo(SortOption.LIKE);
    }

    @Test
    void from_WhenSortIsUpperCase_ThenResolvesToEnumConstant() {
        assertThat(SortOption.from("LATEST")).isEqualTo(SortOption.LATEST);
        assertThat(SortOption.from("LIKE")).isEqualTo(SortOption.LIKE);
    }

    @Test
    void from_WhenSortIsNull_ThenDefaultsToLatest() {
        assertThat(SortOption.from(null)).isEqualTo(SortOption.LATEST);
    }

}
