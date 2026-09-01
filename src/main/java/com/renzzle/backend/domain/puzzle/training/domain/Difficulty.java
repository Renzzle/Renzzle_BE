package com.renzzle.backend.domain.puzzle.training.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Arrays;

@Entity
@Table(name = "difficulty")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Difficulty {

    @Id
    @Column(length = 31)
    private String name;

    private Difficulty(String name) {
        this.name = name;
    }

    public enum DifficultyName {
        HIGH, MIDDLE, LOW
    }

    public static Difficulty getDifficulty(String difficultyName) {
        Difficulty difficulty = new Difficulty();
        difficulty.setDifficulty(difficultyName);
        return difficulty;
    }

    public void setDifficulty(String difficultyName) {
        this.name = Arrays.stream(DifficultyName.values())
                .filter(difficulty -> difficulty.name().equalsIgnoreCase(difficultyName))
                .findFirst()
                .map(Enum::name)
                .orElseThrow(() -> new IllegalArgumentException("Invalid difficulty name: " + difficultyName));
    }
}
