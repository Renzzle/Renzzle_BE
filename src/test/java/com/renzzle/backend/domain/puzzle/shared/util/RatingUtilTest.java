package com.renzzle.backend.domain.puzzle.shared.util;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RatingUtilTest {

    private static final WinColor BLACK = WinColor.getWinColor("BLACK");
    private static final WinColor WHITE = WinColor.getWinColor("WHITE");
    private static final double EPS = 1e-9;

    @DisplayName("depth가 커질수록 레이팅이 단조 증가한다")
    @Test
    void ratingIncreasesWithDepth() {
        double prev = Double.NEGATIVE_INFINITY;
        for (int depth = 1; depth <= 15; depth++) {
            double rating = RatingUtil.puzzleRating(depth, BLACK);
            assertThat(rating).isGreaterThan(prev);
            prev = rating;
        }
    }

    @DisplayName("같은 depth라도 백 승리 퍼즐이 흑 승리보다 레이팅이 높다")
    @Test
    void whiteWinIsRatedHigherThanBlack() {
        for (int depth = 1; depth <= 10; depth++) {
            assertThat(RatingUtil.puzzleRating(depth, WHITE))
                    .isGreaterThan(RatingUtil.puzzleRating(depth, BLACK));
        }
    }

    @DisplayName("depth 5 흑 승리는 기준 레이팅 1000")
    @Test
    void anchorAtDepthFive() {
        assertEquals(1000.0, RatingUtil.puzzleRating(5, BLACK), EPS);
    }

    @DisplayName("아주 깊은 퍼즐은 상한(3000)으로 클램프된다")
    @Test
    void clampsToMax() {
        assertEquals(3000.0, RatingUtil.puzzleRating(100, BLACK), EPS);
        assertEquals(3000.0, RatingUtil.puzzleRating(100, WHITE), EPS);
    }

    @DisplayName("하한(400) 아래로 내려가지 않는다")
    @Test
    void clampsToMin() {
        // depth 0이면 250으로 계산되지만 하한 400으로 클램프
        assertEquals(400.0, RatingUtil.puzzleRating(0, BLACK), EPS);
    }

    @DisplayName("winColor가 null이어도 NPE 없이 동작한다")
    @Test
    void nullWinColorIsSafe() {
        assertEquals(1000.0, RatingUtil.puzzleRating(5, null), EPS);
    }
}
