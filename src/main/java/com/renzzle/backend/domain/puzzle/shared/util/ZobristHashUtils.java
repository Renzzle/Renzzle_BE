package com.renzzle.backend.domain.puzzle.shared.util;

import java.util.Arrays;
import java.util.SplittableRandom;

public final class ZobristHashUtils {

    private static final int BOARD_SIZE = 15;
    private static final int TOTAL_CELLS = BOARD_SIZE * BOARD_SIZE;
    private static final int BLACK = 0;
    private static final int WHITE = 1;
    private static final String INVALID_BOARD_STATUS = "Invalid board status string: ";
    private static final long[][] ZOBRIST_TABLE = initZobristTable();

    private ZobristHashUtils() {
    }

    public static int totalCells() {
        return TOTAL_CELLS;
    }

    public static long hashFromBoardStatus(String boardStatus) {
        return hashFromCellIndexes(parseCellIndexes(boardStatus));
    }

    /**
     * Parses a board status string into 0-based cell indexes in move order.
     * The index of each element is its move index, which determines the stone color.
     */
    public static int[] parseCellIndexes(String boardStatus) {
        if (boardStatus == null || boardStatus.isBlank()) {
            throw new IllegalArgumentException("Board status is null or blank");
        }

        // every move takes at least two characters, so this never overflows
        int[] cellIndexes = new int[boardStatus.length() / 2 + 1];
        int moveCount = 0;

        for (int i = 0; i < boardStatus.length();) {
            int position = parseBoardPosition(boardStatus, i);
            cellIndexes[moveCount++] = position - 1;

            i += ((position - 1) % BOARD_SIZE < 9) ? 2 : 3;
        }

        return Arrays.copyOf(cellIndexes, moveCount);
    }

    public static long hashFromCellIndexes(int[] cellIndexes) {
        long hash = 0L;
        for (int moveIndex = 0; moveIndex < cellIndexes.length; moveIndex++) {
            hash ^= ZOBRIST_TABLE[cellIndexes[moveIndex]][colorOf(moveIndex)];
        }
        return hash;
    }

    /**
     * Incrementally derives the hash of the position reached by adding one stone.
     * Zobrist hashing is XOR-based, so appending a move costs a single XOR
     * instead of re-hashing the whole board.
     *
     * @param hash      hash of the position before the move
     * @param cellIndex 0-based cell the stone is placed on
     * @param moveIndex 0-based index of the new stone, i.e. the stone count before the move
     */
    public static long applyMove(long hash, int cellIndex, int moveIndex) {
        if (cellIndex < 0 || cellIndex >= TOTAL_CELLS) {
            throw new IllegalArgumentException("Invalid cell index: " + cellIndex);
        }
        if (moveIndex < 0) {
            throw new IllegalArgumentException("Invalid move index: " + moveIndex);
        }
        return hash ^ ZOBRIST_TABLE[cellIndex][colorOf(moveIndex)];
    }

    private static int colorOf(int moveIndex) {
        return (moveIndex % 2 == 0) ? BLACK : WHITE;
    }

    private static int parseBoardPosition(String boardStatus, int index) {
        char charPart = boardStatus.charAt(index);
        if (charPart < 'a' || charPart > 'o') {
            throw new IllegalArgumentException(INVALID_BOARD_STATUS + boardStatus);
        }

        int rowBase = (charPart - 'a') * BOARD_SIZE;
        if (index + 1 >= boardStatus.length()) {
            throw new IllegalArgumentException(INVALID_BOARD_STATUS + boardStatus);
        }

        char firstDigit = boardStatus.charAt(index + 1);
        if (firstDigit < '1' || firstDigit > '9') {
            throw new IllegalArgumentException(INVALID_BOARD_STATUS + boardStatus);
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
            throw new IllegalArgumentException(INVALID_BOARD_STATUS + boardStatus);
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
