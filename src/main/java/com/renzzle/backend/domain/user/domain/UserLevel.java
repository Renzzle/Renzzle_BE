package com.renzzle.backend.domain.user.domain;

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
@Table(name = "user_level")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLevel {

    @Id
    @Column(length = 31)
    private String name;

    private UserLevel(String name) {
        this.name = name;
    }

    public enum LevelName {
        BEGINNER, INTERMEDIATE, ADVANCED
    }

    public static UserLevel getDefaultLevel() {
        return new UserLevel(LevelName.BEGINNER.name());
    }

    public UserLevel setLevel(String levelName) {
        LevelName[] levelNames = LevelName.values();
        boolean isValid = Arrays.stream(levelNames)
                .anyMatch(level -> level.name().equals(levelName));
        if (!isValid)
            throw new CustomException(ErrorCode.VALIDATION_ERROR);

        return new UserLevel(levelName); // 새 인스턴스 반환
    }

    public void setLevel(LevelName levelName) {
        this.name = levelName.name();
    }

    public String getName(){
        return name;
    }

}
