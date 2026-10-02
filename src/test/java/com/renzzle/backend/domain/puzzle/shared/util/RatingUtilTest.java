package com.renzzle.backend.domain.puzzle.shared.util;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.training.domain.Difficulty;
import org.junit.jupiter.api.Test;

import static com.renzzle.backend.domain.puzzle.shared.util.RatingUtil.MAX_RATING;
import static com.renzzle.backend.domain.puzzle.shared.util.RatingUtil.MIN_RATING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RatingUtilTest {

    private static final WinColor BLACK = WinColor.getWinColor("BLACK");
    private static final WinColor WHITE = WinColor.getWinColor("WHITE");
    private static final Difficulty LOW = Difficulty.getDifficulty("LOW");
    private static final Difficulty MIDDLE = Difficulty.getDifficulty("MIDDLE");
    private static final Difficulty HIGH = Difficulty.getDifficulty("HIGH");
    private static final double EPS = 1e-9;

    @Test
    void puzzleRating_WhenDepthSevenMiddleBlack_ThenMatchesStartingUserRating() {
        assertEquals(1000.0, RatingUtil.puzzleRating(7, BLACK, MIDDLE), EPS);
    }

    @Test
    void puzzleRating_WhenDepthThirtyOneMiddleBlack_ThenHitsDeepAnchor() {
        assertEquals(2200.0, RatingUtil.puzzleRating(31, BLACK, MIDDLE), EPS);
    }

    @Test
    void puzzleRating_WhenDepthGrows_ThenRisesByEverSmallerSteps() {
        double previous = RatingUtil.puzzleRating(3, BLACK, MIDDLE);
        double previousStep = Double.POSITIVE_INFINITY;
        for (int depth = 5; depth <= 41; depth += 2) {
            double rating = RatingUtil.puzzleRating(depth, BLACK, MIDDLE);
            double step = rating - previous;
            assertThat(step).isPositive().isLessThan(previousStep);
            previous = rating;
            previousStep = step;
        }
    }

    @Test
    void puzzleRating_WhenDepthTwentyOneVersusThirtyOne_ThenGapIsSmallerThanThirteenVersusTwentyOne() {
        double gapLow = RatingUtil.puzzleRating(21, BLACK, MIDDLE) - RatingUtil.puzzleRating(13, BLACK, MIDDLE);
        double gapHigh = RatingUtil.puzzleRating(31, BLACK, MIDDLE) - RatingUtil.puzzleRating(21, BLACK, MIDDLE);

        assertThat(gapHigh).isLessThan(gapLow);
    }

    @Test
    void puzzleRating_WhenPackDifficultyRises_ThenRatesHigher() {
        for (int depth = 3; depth <= 21; depth += 2) {
            assertThat(RatingUtil.puzzleRating(depth, BLACK, LOW))
                    .isLessThan(RatingUtil.puzzleRating(depth, BLACK, MIDDLE));
            assertThat(RatingUtil.puzzleRating(depth, BLACK, MIDDLE))
                    .isLessThan(RatingUtil.puzzleRating(depth, BLACK, HIGH));
        }
    }

    @Test
    void puzzleRating_WhenPuzzleIsShort_ThenPackDifficultyBarelyMovesIt() {
        double shortGap = RatingUtil.puzzleRating(3, BLACK, HIGH) - RatingUtil.puzzleRating(3, BLACK, LOW);
        double deepGap = RatingUtil.puzzleRating(21, BLACK, HIGH) - RatingUtil.puzzleRating(21, BLACK, LOW);

        assertEquals(MIN_RATING, RatingUtil.puzzleRating(1, BLACK, HIGH), EPS);
        assertThat(shortGap).isLessThan(deepGap / 3);
    }

    @Test
    void puzzleRating_WhenNoPack_ThenRatesLikeMiddle() {
        assertEquals(RatingUtil.puzzleRating(9, WHITE, MIDDLE), RatingUtil.puzzleRating(9, WHITE, null), EPS);
    }

    @Test
    void puzzleRating_WhenWhiteWins_ThenAddsFlatOffset() {
        assertEquals(RatingUtil.puzzleRating(9, BLACK, HIGH) + 100.0, RatingUtil.puzzleRating(9, WHITE, HIGH), EPS);
    }

    @Test
    void puzzleRating_WhenPuzzleVeryDeep_ThenClampsToMax() {
        assertEquals(MAX_RATING, RatingUtil.puzzleRating(100, BLACK, MIDDLE), EPS);
        assertEquals(MAX_RATING, RatingUtil.puzzleRating(100, WHITE, HIGH), EPS);
    }

    @Test
    void puzzleRating_WhenDepthIsNotPositive_ThenClampsToMinInsteadOfNaN() {
        assertEquals(MIN_RATING, RatingUtil.puzzleRating(0, BLACK, MIDDLE), EPS);
        assertEquals(MIN_RATING, RatingUtil.puzzleRating(-5, WHITE, HIGH), EPS);
    }

    @Test
    void puzzleRating_WhenWinColorIsNull_ThenRatesLikeBlack() {
        assertEquals(RatingUtil.puzzleRating(7, BLACK, MIDDLE), RatingUtil.puzzleRating(7, null, MIDDLE), EPS);
    }
}
