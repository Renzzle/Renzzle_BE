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
@Table(name = "win_color")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WinColor {

    @Id
    @Column(length = 31)
    private String name;

    private WinColor(String name) {
        this.name = name;
    }

    public enum WinColorName {
        BLACK, WHITE
    }

    public void setWinColor(String winColorName) {
        WinColor.WinColorName[] winColorNames = WinColor.WinColorName.values();
        boolean isValid = Arrays.stream(winColorNames)
                .anyMatch(winColor -> winColor.name().equals(winColorName));
        if(!isValid)
            throw new CustomException(ErrorCode.VALIDATION_ERROR);

        this.name = winColorName;
    }

    public void setWinColor(WinColor.WinColorName winColorName) {
        this.name = winColorName.name();
    }

}
