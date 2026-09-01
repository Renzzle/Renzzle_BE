package com.renzzle.backend.domain.puzzle.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WinColorTest {

    @Test
    void getWinColor_WhenNameIsLowerCase_ThenNormalizesToEnumConstant() {
        // @ValidEnum accepts input case-insensitively, so the factory must too
        assertThat(WinColor.getWinColor("black").getName()).isEqualTo("BLACK");
        assertThat(WinColor.getWinColor("White").getName()).isEqualTo("WHITE");
    }

    @Test
    void getWinColor_WhenNameIsUpperCase_ThenReturnsSameName() {
        assertThat(WinColor.getWinColor("BLACK").getName()).isEqualTo("BLACK");
        assertThat(WinColor.getWinColor("WHITE").getName()).isEqualTo("WHITE");
    }

    @Test
    void getWinColor_WhenNameIsUnknown_ThenThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> WinColor.getWinColor("RED"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WinColor.getWinColor(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getWinColor_WhenNamesDifferOnlyByCase_ThenInstancesAreEqual() {
        assertThat(WinColor.getWinColor("black")).isEqualTo(WinColor.getWinColor("BLACK"));
    }

}
