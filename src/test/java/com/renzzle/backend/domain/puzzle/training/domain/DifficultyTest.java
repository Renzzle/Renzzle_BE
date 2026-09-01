package com.renzzle.backend.domain.puzzle.training.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DifficultyTest {

    @Test
    void getDifficulty_WhenNameIsLowerCase_ThenNormalizesToEnumConstant() {
        assertThat(Difficulty.getDifficulty("low").getName()).isEqualTo("LOW");
        assertThat(Difficulty.getDifficulty("Middle").getName()).isEqualTo("MIDDLE");
        assertThat(Difficulty.getDifficulty("high").getName()).isEqualTo("HIGH");
    }

    @Test
    void getDifficulty_WhenNameIsUpperCase_ThenReturnsSameName() {
        assertThat(Difficulty.getDifficulty("HIGH").getName()).isEqualTo("HIGH");
    }

    @Test
    void getDifficulty_WhenNameIsUnknown_ThenThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> Difficulty.getDifficulty("EXTREME"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Difficulty.getDifficulty(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
