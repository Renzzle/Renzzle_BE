package com.renzzle.backend.global.util;

public class EloUtil {

    private static final double K_MMR = 20.0;
    private static final double K_RATING = 10.0;

    // ELO 를 통한 예상 승률 계산
    public static double expectedWinProbability(double userRating, double problemRating) {
        return 1.0 / (1.0 + Math.pow(10, (problemRating - userRating) / 400.0));
    }

    // 승률에 맞는 문제 Rating을 계산
    public static double getProblemRatingForTargetWinProbability(double userRating, double targetWinProbability) {
        double ratingOffset = 400 * Math.log10((1 - targetWinProbability) / targetWinProbability);
        return userRating + Math.round(ratingOffset);
    }

    // 양수 반환
    public static double calculateMMRIncrease(double userMMR, double problemRating) {
        double expected = expectedWinProbability(userMMR, problemRating);
        return Math.round(K_MMR * (1 - expected));
    }

    // 양수 반환
    public static double calculateRatingIncrease(double userMMR, double problemRating) {
        double expected = expectedWinProbability(userMMR, problemRating);
        return Math.round(K_RATING * (1 - expected));
    }

    // 음수 반환
    public static double calculateMMRDecrease(double userMMR, double problemRating) {
        double expected = expectedWinProbability(userMMR, problemRating);
        return Math.round(-K_MMR * (1 - expected));
    }

    // 음수 반환
    public static double calculateRatingDecrease(double userMMR, double problemRating) {
        double expected = expectedWinProbability(userMMR, problemRating);
        return Math.round(-K_RATING * (1 - expected));
    }
}
