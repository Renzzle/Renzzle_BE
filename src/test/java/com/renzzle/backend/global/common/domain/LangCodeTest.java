package com.renzzle.backend.global.common.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LangCodeTest {

    @Test
    void getLangCode_WhenNameIsLowerCase_ThenNormalizesToEnumConstant() {
        // @ValidEnum accepts input case-insensitively, so the factory must too
        assertThat(LangCode.getLangCode("ko").getName()).isEqualTo("KO");
        assertThat(LangCode.getLangCode("en").getName()).isEqualTo("EN");
        assertThat(LangCode.getLangCode("Ko").getName()).isEqualTo("KO");
    }

    @Test
    void getLangCode_WhenNameIsUpperCase_ThenReturnsSameName() {
        assertThat(LangCode.getLangCode("KO").getName()).isEqualTo("KO");
        assertThat(LangCode.getLangCode("EN").getName()).isEqualTo("EN");
    }

    @Test
    void getLangCode_WhenNameIsUnknown_ThenThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> LangCode.getLangCode("JP"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LangCode.getLangCode((String) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
