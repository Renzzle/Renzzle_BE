package com.renzzle.backend.util;

import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils.CopyMatch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class BoardUtilsTest {

    private static final Pattern STONE = Pattern.compile("([a-o])(\\d+)");

    // Black h8-h10 against white i8-i10; black h11, white h12, black h7 makes five
    private static final String PUZZLE_BOARD = "h8i8h9i9h10i10";
    private static final String PUZZLE_ANSWER = "h11h12h7";

    @Test
    void getBoardPositionFromStringTest() throws Exception {
        // get private method
        Method method = BoardUtils.class.getDeclaredMethod("getBoardPositionFromString", String.class, int.class);
        method.setAccessible(true);

        // b15 == 30
        int position = (int) method.invoke(null, "a1b15c7", 2);
        Assertions.assertEquals(30, position);

        // a1 == 1
        position = (int) method.invoke(null, "a1b15c7", 0);
        Assertions.assertEquals(1, position);

        // c7 == 37
        position = (int) method.invoke(null, "a1b15c7", 5);
        Assertions.assertEquals(37, position);

        // number part more than 15
        Exception exception = Assertions.assertThrows(InvocationTargetException.class, () -> {
            method.invoke(null, "b16", 0);
        });
        Throwable cause = exception.getCause();
        Assertions.assertInstanceOf(IllegalArgumentException.class, cause);

        // number part more than 15
        exception = Assertions.assertThrows(InvocationTargetException.class, () -> {
            method.invoke(null, "b1111", 0);
        });
        cause = exception.getCause();
        Assertions.assertInstanceOf(IllegalArgumentException.class, cause);

        // no number part
        exception = Assertions.assertThrows(InvocationTargetException.class, () -> {
            method.invoke(null, "b", 0);
        });
        cause = exception.getCause();
        Assertions.assertInstanceOf(IllegalArgumentException.class, cause);

        // character part invalid character
        exception = Assertions.assertThrows(InvocationTargetException.class, () -> {
            method.invoke(null, "r13", 0);
        });
        cause = exception.getCause();
        Assertions.assertInstanceOf(IllegalArgumentException.class, cause);

        // number part invalid structure
        exception = Assertions.assertThrows(InvocationTargetException.class, () -> {
            method.invoke(null, "b03", 0);
        });
        cause = exception.getCause();
        Assertions.assertInstanceOf(IllegalArgumentException.class, cause);
    }

    @Test
    void rotate90Test() throws Exception {
        Method getBoardPositionFromString = BoardUtils.class.getDeclaredMethod("getBoardPositionFromString", String.class, int.class);
        getBoardPositionFromString.setAccessible(true);

        Method rotate90 = BoardUtils.class.getDeclaredMethod("rotate90", int.class);
        rotate90.setAccessible(true);

        // case 1
        int pos = (int) getBoardPositionFromString.invoke(null, "a1", 0);
        int pos90 = (int) getBoardPositionFromString.invoke(null, "a15", 0);
        int resultPos = (int) rotate90.invoke(null, pos);
        Assertions.assertEquals(pos90, resultPos);

        // case 2
        pos = (int) getBoardPositionFromString.invoke(null, "g5", 0);
        pos90 = (int) getBoardPositionFromString.invoke(null, "e9", 0);
        resultPos = (int) rotate90.invoke(null, pos);
        Assertions.assertEquals(pos90, resultPos);

        // case 3
        pos = (int) getBoardPositionFromString.invoke(null, "h8", 0);
        pos90 = (int) getBoardPositionFromString.invoke(null, "h8", 0);
        resultPos = (int) rotate90.invoke(null, pos);
        Assertions.assertEquals(pos90, resultPos);
    }

    @Test
    void xAxisSymmetryTest() throws Exception {
        Method getBoardPositionFromString = BoardUtils.class.getDeclaredMethod("getBoardPositionFromString", String.class, int.class);
        getBoardPositionFromString.setAccessible(true);

        Method xAxisSymmetry = BoardUtils.class.getDeclaredMethod("xAxisSymmetry", int.class);
        xAxisSymmetry.setAccessible(true);

        // case 1
        int pos = (int) getBoardPositionFromString.invoke(null, "a1", 0);
        int posXSymmetry = (int) getBoardPositionFromString.invoke(null, "a15", 0);
        int resultPos = (int) xAxisSymmetry.invoke(null, pos);
        Assertions.assertEquals(posXSymmetry, resultPos);

        // case 2
        pos = (int) getBoardPositionFromString.invoke(null, "g3", 0);
        posXSymmetry = (int) getBoardPositionFromString.invoke(null, "g13", 0);
        resultPos = (int) xAxisSymmetry.invoke(null, pos);
        Assertions.assertEquals(posXSymmetry, resultPos);

        // case 3
        pos = (int) getBoardPositionFromString.invoke(null, "h8", 0);
        posXSymmetry = (int) getBoardPositionFromString.invoke(null, "h8", 0);
        resultPos = (int) xAxisSymmetry.invoke(null, pos);
        Assertions.assertEquals(posXSymmetry, resultPos);
    }

    @Test
    void makeBoardKeyTest() {
        String s1 = BoardUtils.makeBoardKey("h8i9i7h7j8i8j9k9");
        String s2 = BoardUtils.makeBoardKey("i7k9j8h7h8i8j9i9");
        Assertions.assertEquals(s1, s2);

        s1 = BoardUtils.makeBoardKey("h8j9h7");
        s2 = BoardUtils.makeBoardKey("h8j9h7h6h5h4h3h2a11n7");
        Assertions.assertNotEquals(s1, s2);
    }

    @Test
    void makeBoardKey_WhenBoardIsRotatedOrMirrored_ThenKeyIsUnchanged() {
        List<String> boards = allSymmetries("h8h10i9k11g6");
        Assertions.assertEquals(8, boards.stream().distinct().count());

        String key = BoardUtils.makeBoardKey(boards.get(0));
        for (String board : boards) {
            Assertions.assertEquals(key, BoardUtils.makeBoardKey(board), board);
        }
    }

    @Test
    void makeBoardKey_WhenOnlyOneColorIsMirrored_ThenKeysDiffer() {
        // Mirroring white h10 to h6 alone is not a symmetry of the whole board
        Assertions.assertNotEquals(BoardUtils.makeBoardKey("h8h10i9"), BoardUtils.makeBoardKey("h8h6i9"));
    }

    @Test
    void makeBoardKey_WhenCellNumbersRunTogetherAlike_ThenKeysDiffer() {
        // Black [1, 23] + white [4] and black [1, 2] + white [34] both read "1234" without delimiters
        Assertions.assertNotEquals(BoardUtils.makeBoardKey("a1a4b8"), BoardUtils.makeBoardKey("a1c4a2"));
    }

    @Test
    void makeAnswerKey_WhenAnswerIsTurnedOrShifted_ThenKeyIsUnchanged() {
        String key = BoardUtils.makeAnswerKey(PUZZLE_BOARD, PUZZLE_ANSWER);

        // Transposed, then the transposed line moved one row and one column
        Assertions.assertEquals(key, BoardUtils.makeAnswerKey(PUZZLE_BOARD, "k8l8g8"));
        Assertions.assertEquals(key, BoardUtils.makeAnswerKey(PUZZLE_BOARD, "l9m9h9"));
    }

    @Test
    void makeAnswerKey_WhenTheOtherColorMovesFirst_ThenKeysDiffer() {
        Assertions.assertNotEquals(
                BoardUtils.makeAnswerKey(PUZZLE_BOARD, PUZZLE_ANSWER),
                BoardUtils.makeAnswerKey(PUZZLE_BOARD + "a1", PUZZLE_ANSWER));
    }

    @Test
    void matchCopy_WhenFewStonesAreAdded_ThenNearCopy() {
        Assertions.assertEquals(CopyMatch.NEAR_COPY, BoardUtils.matchCopy(
                PUZZLE_BOARD + "j11k11", PUZZLE_ANSWER, PUZZLE_BOARD, PUZZLE_ANSWER));
    }

    @Test
    void matchCopy_WhenCopyIsTurnedShiftedAndPadded_ThenNearCopy() {
        // A quarter turn that puts the first answer move on e5, plus a far pair of stones
        Assertions.assertEquals(CopyMatch.NEAR_COPY, BoardUtils.matchCopy(
                "b5b4c5c4d5d4o1o15", "e5f5a5", PUZZLE_BOARD, PUZZLE_ANSWER));
    }

    @Test
    void matchCopy_WhenMostStonesAreKept_ThenNearCopy() {
        // Eight of the original ten stones
        Assertions.assertEquals(CopyMatch.NEAR_COPY, BoardUtils.matchCopy(
                PUZZLE_BOARD + "f9g9", PUZZLE_ANSWER, PUZZLE_BOARD + "f8g8f9g9", PUZZLE_ANSWER));
    }

    @Test
    void matchCopy_WhenOneStoneIsMoved_ThenNearCopy() {
        // White i10 moved to i11, so neither board holds the other
        Assertions.assertEquals(CopyMatch.NEAR_COPY, BoardUtils.matchCopy(
                "h8i8h9i9h10i11", PUZZLE_ANSWER, PUZZLE_BOARD, PUZZLE_ANSWER));
    }

    @Test
    void matchCopy_WhenAddedStonesReachEightyPercentOfOriginal_ThenHeavilyPadded() {
        String original = PUZZLE_BOARD + "f8g8f9g9";
        // Six new stones on a ten-stone original
        Assertions.assertEquals(CopyMatch.NEAR_COPY, BoardUtils.matchCopy(
                original + "j11k11j12k12j13k13", PUZZLE_ANSWER, original, PUZZLE_ANSWER));
        // Eight new stones
        Assertions.assertEquals(CopyMatch.HEAVILY_PADDED, BoardUtils.matchCopy(
                original + "j11k11j12k12j13k13j6k6", PUZZLE_ANSWER, original, PUZZLE_ANSWER));
    }

    @Test
    void matchCopy_WhenStonesAreAddedOnlyBeyondReachOfAnswer_ThenNearCopy() {
        // Eight stones far from the answer
        Assertions.assertEquals(CopyMatch.NEAR_COPY, BoardUtils.matchCopy(
                PUZZLE_BOARD + "a1o15a15o1b1n15b15n1", PUZZLE_ANSWER, PUZZLE_BOARD, PUZZLE_ANSWER));
    }

    @Test
    void matchCopy_WhenAnswerOrStonesDiffer_ThenNone() {
        // Answer reordered, two of six stones swapping colors, the other color moving first
        Assertions.assertEquals(CopyMatch.NONE, BoardUtils.matchCopy(
                PUZZLE_BOARD, "h11h7h12", PUZZLE_BOARD, PUZZLE_ANSWER));
        Assertions.assertEquals(CopyMatch.NONE, BoardUtils.matchCopy(
                "h8i8h9i9i10h10", PUZZLE_ANSWER, PUZZLE_BOARD, PUZZLE_ANSWER));
        Assertions.assertEquals(CopyMatch.NONE, BoardUtils.matchCopy(
                PUZZLE_BOARD + "a1", PUZZLE_ANSWER, PUZZLE_BOARD, PUZZLE_ANSWER));
    }

    @Test
    void matchCopy_WhenOriginalHasFewStones_ThenOnlyNearlyEqualBoardsAreNearCopies() {
        Assertions.assertEquals(CopyMatch.NEAR_COPY, BoardUtils.matchCopy(
                "h8i8h9i9j9k9", "h10h11h7", "h8i8h9i9", "h10h11h7"));
        Assertions.assertEquals(CopyMatch.HEAVILY_PADDED, BoardUtils.matchCopy(
                "h8i8h9i9j9k9j10k10", "h10h11h7", "h8i8h9i9", "h10h11h7"));
        // Shifted one column, nothing added
        Assertions.assertEquals(CopyMatch.NEAR_COPY, BoardUtils.matchCopy(
                "h9i9h10i10", "h11h12h8", "h8i8h9i9", "h10h11h7"));
    }

    @Test
    void matchCopy_WhenOriginalIsFarLarger_ThenNone() {
        // Six of the original ten stones
        Assertions.assertEquals(CopyMatch.NONE, BoardUtils.matchCopy(
                PUZZLE_BOARD, PUZZLE_ANSWER, PUZZLE_BOARD + "f8g8f9g9", PUZZLE_ANSWER));
    }

    // The board in all four rotations, each also mirrored
    private static List<String> allSymmetries(String board) {
        List<String> boards = new ArrayList<>();
        String rotated = board;
        for (int i = 0; i < 4; i++) {
            boards.add(rotated);
            boards.add(transform(rotated, (row, col) -> row * 15 + (14 - col)));
            rotated = transform(rotated, (row, col) -> col * 15 + (14 - row));
        }
        return boards;
    }

    // Moves every stone to the cell index the mapping returns, keeping the move order
    private static String transform(String board, IntBinaryOperator cellOf) {
        StringBuilder result = new StringBuilder();
        Matcher stone = STONE.matcher(board);
        while (stone.find()) {
            int cell = cellOf.applyAsInt(stone.group(1).charAt(0) - 'a', Integer.parseInt(stone.group(2)) - 1);
            result.append((char) ('a' + cell / 15)).append(cell % 15 + 1);
        }
        return result.toString();
    }

}
