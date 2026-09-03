package com.renzzle.backend.domain.puzzle.shared.util;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;

public class RatingUtil {

    private RatingUtil() {}

    private static final double BASE_RATING = 400.0;
    private static final double PER_DEPTH = 150.0;
    private static final double WHITE_WIN_OFFSET = 100.0;
    private static final double MIN_RATING = 400.0;
    private static final double MAX_RATING = 3000.0;

    public static double puzzleRating(int depth, WinColor winColor) {
        double rating = BASE_RATING + (depth - 1) * PER_DEPTH;

        if (winColor != null && WinColor.WinColorName.WHITE.name().equals(winColor.getName())) {
            rating += WHITE_WIN_OFFSET;
        }

        return Math.max(MIN_RATING, Math.min(MAX_RATING, rating));
    }

}
