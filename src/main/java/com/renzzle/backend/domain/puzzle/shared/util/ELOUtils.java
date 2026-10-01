package com.renzzle.backend.domain.puzzle.shared.util;

public class ELOUtils {

    private ELOUtils() {}

    private static final double K_MMR = 20.0;
    private static final double K_RATING = 10.0;
    private static final double MMR_THRESHOLD = 1500.0;
    private static final double HIGH_REWARD = 0.5;
    private static final double LOW_REWARD = 1.5;
    public static final double TARGET_WIN_PROBABILITY = 0.7;
    public static final double WIN_PROBABILITY_DELTA = 0.05;
    // Outside (0, 1) the rating offset turns NaN and silently collapses to the user's own rating
    public static final double MIN_TARGET_WIN_PROBABILITY = 0.05;
    public static final double MAX_TARGET_WIN_PROBABILITY = 0.95;
    // A new puzzle's rating is only a guess from its depth, so K starts high and falls as results
    // come in: half of K_PUZZLE_MAX after K_PUZZLE_HALVING_ATTEMPTS results, never below K_PUZZLE_MIN
    private static final double K_PUZZLE_MAX = 40.0;
    private static final double K_PUZZLE_MIN = 8.0;
    private static final double K_PUZZLE_HALVING_ATTEMPTS = 10.0;

    private static double getRewardMultiplier(double userMmr) {
        return userMmr >= MMR_THRESHOLD ? HIGH_REWARD : LOW_REWARD;
    }

    private static double getPenaltyMultiplier(double userMmr) {
        return userMmr >= MMR_THRESHOLD ? LOW_REWARD : HIGH_REWARD;
    }

    // Calculate expected win probability via ELO
    public static double expectedWinProbability(double userRating, double problemRating) {
        return 1.0 / (1.0 + Math.pow(10, (problemRating - userRating) / 400.0));
    }

    // Step the target win probability after a round: harder after a solve, easier after a miss
    public static double nextTargetWinProbability(double current, boolean solved) {
        double next = solved ? current - WIN_PROBABILITY_DELTA : current + WIN_PROBABILITY_DELTA;
        return Math.max(MIN_TARGET_WIN_PROBABILITY, Math.min(MAX_TARGET_WIN_PROBABILITY, next));
    }

    // Calculate the problem Rating matching the target win probability based on the user's rating
    public static double getProblemRatingForTargetWinProbability(double userRating, double targetWinProbability) {
        double ratingOffset = 400 * Math.log10((1 - targetWinProbability) / targetWinProbability);
        return userRating + Math.round(ratingOffset);
    }

    // On a win, gain K * (1 - expected): the bigger the upset, the bigger the gain
    public static double calculateMMRIncrease(double userMMR, double problemRating) {
        double expected = expectedWinProbability(userMMR, problemRating);
        return Math.round(K_MMR * (1 - expected) * getRewardMultiplier(userMMR));
    }

    public static double calculateRatingIncrease(double userRating, double problemRating) {
        double expected = expectedWinProbability(userRating, problemRating);
        return Math.round(K_RATING * (1 - expected) * getRewardMultiplier(userRating));
    }

    // On a loss, lose K * expected: the more certain the win was, the bigger the loss
    public static double calculateMMRDecrease(double userMMR, double problemRating) {
        double expected = expectedWinProbability(userMMR, problemRating);
        return Math.round(-K_MMR * expected * getPenaltyMultiplier(userMMR));
    }

    public static double calculateRatingDecrease(double userRating, double problemRating) {
        double expected = expectedWinProbability(userRating, problemRating);
        return Math.round(-K_RATING * expected * getPenaltyMultiplier(userRating));
    }

    public static double puzzleKFactor(int rankAttemptCount) {
        double k = K_PUZZLE_MAX * K_PUZZLE_HALVING_ATTEMPTS / (K_PUZZLE_HALVING_ATTEMPTS + rankAttemptCount);
        return Math.max(K_PUZZLE_MIN, k);
    }

    public static double calculatePuzzleRatingChange(
            double userMmr, double puzzleRating, int rankAttemptCount, boolean solved) {
        double expected = expectedWinProbability(userMmr, puzzleRating);
        double k = puzzleKFactor(rankAttemptCount);
        return solved ? -k * (1 - expected) : k * expected;
    }
}
