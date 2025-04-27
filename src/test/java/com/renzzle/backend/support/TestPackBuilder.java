package com.renzzle.backend.support;

import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;

public class TestPackBuilder {

    private Long id = null;
    private int puzzleCount = 1;
    private int price = 0;
    private Difficulty difficulty = Difficulty.getDifficulty("LOW");

    public static TestPackBuilder builder() {
        return new TestPackBuilder();
    }

    public TestPackBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public TestPackBuilder withPuzzleCount(int puzzleCount) {
        this.puzzleCount = puzzleCount;
        return this;
    }

    public TestPackBuilder withPrice(int price) {
        this.price = price;
        return this;
    }

    public TestPackBuilder withDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    public Pack build() {
        return Pack.builder()
                .id(id)
                .puzzleCount(puzzleCount)
                .price(price)
                .difficulty(difficulty)
                .build();
    }
}
