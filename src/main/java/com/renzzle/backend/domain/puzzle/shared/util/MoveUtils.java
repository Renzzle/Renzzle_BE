package com.renzzle.backend.domain.puzzle.shared.util;

public final class MoveUtils {

    private static final int BOARD_SIZE = 15;

    private MoveUtils() {}

    /**
     * AI 엔진이 반환하는 int 값을 보드 좌표 문자열로 변환한다.
     * move / 15 → 알파벳(a~o), move % 15 → 숫자(1~15)
     * 예: 112 → "h8"
     */
    public static String convertMoveToPosition(int move) {
        if (move < 0 || move >= BOARD_SIZE * BOARD_SIZE) {
            throw new IllegalArgumentException("Invalid move value: " + move);
        }
        char letter = (char) ('a' + move / BOARD_SIZE);
        int number = move % BOARD_SIZE + 1;
        return "" + letter + number;
    }
}
