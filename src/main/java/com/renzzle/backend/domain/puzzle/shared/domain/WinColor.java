package com.renzzle.backend.domain.puzzle.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Arrays;
import java.util.Objects;

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
        this.name = Arrays.stream(WinColorName.values())
                .filter(winColor -> winColor.name().equalsIgnoreCase(winColorName))
                .findFirst()
                .map(Enum::name)
                .orElseThrow(() -> new IllegalArgumentException("Invalid win color name: " + winColorName));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WinColor winColor) {
            return this.name.equals(winColor.name);
        } else return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

}
