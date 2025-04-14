package com.renzzle.backend.domain.puzzle.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Arrays;

@Entity
@Table(name = "win_color")
@Getter
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

    public static WinColor getWinColor(String winColorName) {
        WinColor winColor = new WinColor();
        winColor.setWinColor(winColorName);
        return winColor;
    }

    public void setWinColor(String winColorName) {
        WinColor.WinColorName[] winColorNames = WinColor.WinColorName.values();
        boolean isValid = Arrays.stream(winColorNames)
                .anyMatch(winColor -> winColor.name().equals(winColorName));
        if(!isValid)
            throw new IllegalArgumentException("Invalid win color name: " + winColorName);

        this.name = winColorName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WinColor) {
            return this.name.equals(((WinColor) obj).name);
        } else return false;
    }
}
