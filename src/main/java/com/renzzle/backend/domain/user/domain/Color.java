package com.renzzle.backend.domain.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import java.util.Random;

@Entity
@Table(name = "color")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Color {

    private static final Random RANDOM = new Random();

    @Id
    @Column(length = 31)
    private String name;

    private Color(String name) {
        this.name = name;
    }

    public enum ColorName {
        RED, ORANGE, GREEN, BLUE, INDIGO, PURPLE,
        DARK_RED, DARK_ORANGE, DARK_GREEN,
        DARK_BLUE, DARK_INDIGO, DARK_PURPLE
    }

    public static Color getRandomColor() {
        ColorName[] colorNames = ColorName.values();
        int randomIdx = RANDOM.nextInt(colorNames.length);
        ColorName randomColorName = colorNames[randomIdx];

        return new Color(randomColorName.name());
    }

    public String getName() {
        return name;
    }

}
