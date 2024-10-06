package com.renzzle.backend.domain.puzzle.domain;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.util.Arrays;

@Entity
@Table(name = "difficulty")
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
        Difficulty.DifficultyName[] difficultyNames = Difficulty.DifficultyName.values();
        boolean isValid = Arrays.stream(difficultyNames)
                .anyMatch(difficulty -> difficulty.name().equals(difficultyName));
        if(!isValid)
            throw new IllegalArgumentException("Invalid difficulty name: " + difficultyName);

        this.name = difficultyName;
    }

}
