package com.renzzle.backend.util;

import com.renzzle.backend.global.util.BoardUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BoardUtilsTest {

    @Test
    public void getBoardPositionFromStringTest() throws Exception {
        // get private method
        Method method = BoardUtils.class.getDeclaredMethod("getBoardPositionFromString", String.class, int.class);
        method.setAccessible(true);

        // b15 == 30
        int position = (int) method.invoke(null, "a1b15c7", 2);
        Assertions.assertEquals(position, 30);

        // a1 == 1
        position = (int) method.invoke(null, "a1b15c7", 0);
        Assertions.assertEquals(position, 1);

        // c7 == 37
        position = (int) method.invoke(null, "a1b15c7", 5);
        Assertions.assertEquals(position, 37);

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
    public void rotate90Test() throws Exception {
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
    public void xAxisSymmetryTest() throws Exception {
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
    public void makeBoardKeyTest() {
        String s1 = BoardUtils.makeBoardKey("h8i9i7h7j8i8j9k9");
        String s2 = BoardUtils.makeBoardKey("i7k9j8h7h8i8j9i9");
        Assertions.assertEquals(s1, s2);

        s1 = BoardUtils.makeBoardKey("h8j9h7");
        s2 = BoardUtils.makeBoardKey("h8j9h7h6h5h4h3h2a11n7");
        Assertions.assertNotEquals(s1, s2);
    }

}
