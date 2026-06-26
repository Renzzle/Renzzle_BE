package com.renzzle.backend.domain.puzzle.shared.util;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;

public class RatingUtil {

    /*
        퍼즐 생성/수정 시 난이도(depth)와 승리색(winColor)으로 레이팅을 산정한다.
        유저 레이팅과 동일한 ELO 축(기본 1000) 위에 놓이도록 설계되어 랭킹전 매칭에 그대로 쓰인다.
        ※ 실제 사용자 요구사항 확정 전까지의 잠정 공식 — 아래 상수만 조정해 튜닝한다.
     */

    private static final double BASE_RATING = 400.0;       // depth 1 기준 레이팅
    private static final double PER_DEPTH = 150.0;         // depth 1 증가당 상승폭 (depth 5 ≈ 1000)
    private static final double WHITE_WIN_OFFSET = 100.0;  // 백 승리 퍼즐을 약간 더 어렵게 가정(잠정 — 필요 시 부호/크기 조정)
    private static final double MIN_RATING = 400.0;        // 하한
    private static final double MAX_RATING = 3000.0;       // 상한 (깊은 퍼즐의 과도한 레이팅 방지)

    public static double puzzleRating(int depth, WinColor winColor) {
        double rating = BASE_RATING + (depth - 1) * PER_DEPTH;

        if (winColor != null && WinColor.WinColorName.WHITE.name().equals(winColor.getName())) {
            rating += WHITE_WIN_OFFSET;
        }

        return Math.max(MIN_RATING, Math.min(MAX_RATING, rating));
    }

}
