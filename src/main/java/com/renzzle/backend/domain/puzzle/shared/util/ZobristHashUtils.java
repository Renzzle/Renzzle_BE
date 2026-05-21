package com.renzzle.backend.domain.puzzle.shared.util;

import java.util.SplittableRandom;

public final class ZobristHashUtils {

    private static final int BOARD_SIZE = 15;
    private static final int TOTAL_CELLS = BOARD_SIZE * BOARD_SIZE;
    private static final int BLACK = 0;
    private static final int WHITE = 1;
    private static final long[][] ZOBRIST_TABLE = initZobristTable();

    private ZobristHashUtils() {
    }

    public static long hashFromBoardStatus(String boardStatus) {
        if (boardStatus == null || boardStatus.isBlank()) {
            throw new IllegalArgumentException("Board status is null or blank");
        }

        long hash = 0L;
        int moveIndex = 0;

        for (int i = 0; i < boardStatus.length();) {
            int position = parseBoardPosition(boardStatus, i);
            int cellIndex = position - 1;
            int stoneColor = (moveIndex % 2 == 0) ? BLACK : WHITE;
            hash ^= ZOBRIST_TABLE[cellIndex][stoneColor];

            i += ((position - 1) % BOARD_SIZE < 9) ? 2 : 3;
            moveIndex++;
        }

        return hash;
    }

    private static int parseBoardPosition(String boardStatus, int index) {
        char charPart = boardStatus.charAt(index);
        if (charPart < 'a' || charPart > 'o') {
            throw new IllegalArgumentException("Invalid board status string: " + boardStatus);
        }

        int rowBase = (charPart - 'a') * BOARD_SIZE;
        if (index + 1 >= boardStatus.length()) {
            throw new IllegalArgumentException("Invalid board status string: " + boardStatus);
        }

        char firstDigit = boardStatus.charAt(index + 1);
        if (firstDigit < '1' || firstDigit > '9') {
            throw new IllegalArgumentException("Invalid board status string: " + boardStatus);
        }

        int digitsNum = 0;
        while (index + 1 + digitsNum < boardStatus.length()) {
            char c = boardStatus.charAt(index + 1 + digitsNum);
            if (c < '0' || c > '9') {
                break;
            }
            digitsNum++;
        }

        int col = Integer.parseInt(boardStatus.substring(index + 1, index + 1 + digitsNum));
        if (col < 1 || col > BOARD_SIZE) {
            throw new IllegalArgumentException("Invalid board status string: " + boardStatus);
        }

        return rowBase + col;
    }

    private static long[][] initZobristTable() {
        long[][] table = new long[TOTAL_CELLS][2];
        SplittableRandom random = new SplittableRandom(20260308L);

        for (int i = 0; i < TOTAL_CELLS; i++) {
            table[i][BLACK] = random.nextLong();
            table[i][WHITE] = random.nextLong();
        }
        return table;
    }
}
