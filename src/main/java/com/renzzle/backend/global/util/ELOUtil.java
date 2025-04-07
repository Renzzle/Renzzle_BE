package com.renzzle.backend.global.util;

public class ELOUtil {

    private static final double K_MMR = 20.0;
    private static final double K_RATING = 10.0;
    private static final double MMR_THRESHOLD = 1500.0;
    private static final double HIGH_REWARD = 0.5;
    private static final double LOW_REWARD = 1.5;
    public static final double TARGET_WIN_PROBABILITY = 0.7;
    public static final double WIN_PROBABILITY_DELTA = 0.05;

    private static double getRewardMultiplier(double userMmr) {
        return userMmr >= MMR_THRESHOLD ? HIGH_REWARD : LOW_REWARD;
    }

    private static double getPenaltyMultiplier(double userMmr) {
        return userMmr >= MMR_THRESHOLD ? LOW_REWARD : HIGH_REWARD;
    }

    // ELO 를 통한 예상 승률 계산
    public static double expectedWinProbability(double userRating, double problemRating) {
        return 1.0 / (1.0 + Math.pow(10, (problemRating - userRating) / 400.0));
    }

    // 사용자 레이팅 기준 승률에 맞는 문제 Rating을 계산
    public static double getProblemRatingForTargetWinProbability(double userRating, double targetWinProbability) {
        double ratingOffset = 400 * Math.log10((1 - targetWinProbability) / targetWinProbability);
        return userRating + Math.round(ratingOffset);
    }

    // 양수 반환
    public static double calculateMMRIncrease(double userMMR, double problemRating) {
        double expected = expectedWinProbability(userMMR, problemRating);
        return Math.round(K_MMR * (1 - expected) * getRewardMultiplier(userMMR));
    }

    // 양수 반환
    public static double calculateRatingIncrease(double userRating, double problemRating) {
        double expected = expectedWinProbability(userRating, problemRating);
        return Math.round(K_RATING * (1 - expected) * getRewardMultiplier(userRating));
    }

    // 음수 반환
    public static double calculateMMRDecrease(double userMMR, double problemRating) {
        double expected = expectedWinProbability(userMMR, problemRating);
        return Math.round(-K_MMR * (1 - expected) * getPenaltyMultiplier(userMMR));
    }

    // 음수 반환
    public static double calculateRatingDecrease(double userRating, double problemRating) {
        double expected = expectedWinProbability(userRating, problemRating);
        return Math.round(-K_RATING * (1 - expected) * getPenaltyMultiplier(userRating));
    }
}
