package com.renzzle.backend.util;

import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
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
