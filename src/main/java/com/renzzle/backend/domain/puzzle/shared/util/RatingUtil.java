package com.renzzle.backend.domain.puzzle.shared.util;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;

public class RatingUtil {

    private RatingUtil() {}

    public static final double MIN_RATING = 100.0;
    public static final double MAX_RATING = 3000.0;

    // Depth 7 sits at the starting user rating and depth 31 at 2200, growing with ln(depth + 1) in between
    private static final double ANCHOR_DEPTH = 7;
    private static final double ANCHOR_RATING = 1000.0;
    private static final double DEEP_ANCHOR_DEPTH = 31;
    private static final double DEEP_ANCHOR_RATING = 2200.0;
    private static final double DEPTH_SCALE = (DEEP_ANCHOR_RATING - ANCHOR_RATING)
            / Math.log((DEEP_ANCHOR_DEPTH + 1) / (ANCHOR_DEPTH + 1));

    private static final double LOW_MULTIPLIER = 0.8;
    private static final double MIDDLE_MULTIPLIER = 1.0;
    private static final double HIGH_MULTIPLIER = 1.25;
    private static final double WHITE_WIN_OFFSET = 100.0;

    // Community puzzles have no pack, so a null difficulty rates like MIDDLE
    public static double puzzleRating(int depth, WinColor winColor, Difficulty difficulty) {
        double base = ANCHOR_RATING + DEPTH_SCALE * Math.log((Math.max(depth, 1) + 1) / (ANCHOR_DEPTH + 1));
        double rating = base * difficultyMultiplier(difficulty);

        if (winColor != null && WinColor.WinColorName.WHITE.name().equals(winColor.getName())) {
            rating += WHITE_WIN_OFFSET;
        }

        return Math.max(MIN_RATING, Math.min(MAX_RATING, rating));
    }

    private static double difficultyMultiplier(Difficulty difficulty) {
        if (difficulty == null) {
            return MIDDLE_MULTIPLIER;
        }
        return switch (Difficulty.DifficultyName.valueOf(difficulty.getName())) {
            case LOW -> LOW_MULTIPLIER;
            case MIDDLE -> MIDDLE_MULTIPLIER;
            case HIGH -> HIGH_MULTIPLIER;
        };
    }

}
