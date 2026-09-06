package com.renzzle.backend.domain.puzzle.shared.util;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RatingUtilTest {

    private static final WinColor BLACK = WinColor.getWinColor("BLACK");
    private static final WinColor WHITE = WinColor.getWinColor("WHITE");
    private static final double EPS = 1e-9;

    @Test
    void puzzleRating_WhenDepthGrows_ThenIncreasesMonotonically() {
        double prev = Double.NEGATIVE_INFINITY;
        for (int depth = 1; depth <= 15; depth++) {
            double rating = RatingUtil.puzzleRating(depth, BLACK);
            assertThat(rating).isGreaterThan(prev);
            prev = rating;
        }
    }

    @Test
    void puzzleRating_WhenWhiteWinsAtSameDepth_ThenRatesHigherThanBlack() {
        for (int depth = 1; depth <= 10; depth++) {
            assertThat(RatingUtil.puzzleRating(depth, WHITE))
                    .isGreaterThan(RatingUtil.puzzleRating(depth, BLACK));
        }
    }

    @Test
    void puzzleRating_WhenDepthFiveBlackWin_ThenReturnsAnchorOf1000() {
        assertEquals(1000.0, RatingUtil.puzzleRating(5, BLACK), EPS);
    }

    @Test
    void puzzleRating_WhenPuzzleVeryDeep_ThenClampsToMax() {
        assertEquals(3000.0, RatingUtil.puzzleRating(100, BLACK), EPS);
        assertEquals(3000.0, RatingUtil.puzzleRating(100, WHITE), EPS);
    }

    @Test
    void puzzleRating_WhenBelowFloor_ThenClampsToMin() {
        // depth 0 computes to 250 but is clamped up to the floor of 400
        assertEquals(400.0, RatingUtil.puzzleRating(0, BLACK), EPS);
    }

    @Test
    void puzzleRating_WhenWinColorIsNull_ThenDoesNotThrow() {
        assertEquals(1000.0, RatingUtil.puzzleRating(5, null), EPS);
    }
}
