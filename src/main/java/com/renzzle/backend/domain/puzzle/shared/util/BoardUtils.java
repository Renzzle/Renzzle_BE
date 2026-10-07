package com.renzzle.backend.domain.puzzle.shared.util;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BoardUtils {

    private static final int SYMMETRY_COUNT = 8;

    private BoardUtils() {}

    public static String makeBoardKey(String boardStatus) {
        if(boardStatus == null || boardStatus.isBlank())
            throwIllegalBoardStatusException(boardStatus);

        List<List<Integer>> blackPosLists = createSymmetryLists();
        List<List<Integer>> whitePosLists = createSymmetryLists();

        parseBoardStatus(boardStatus, blackPosLists, whitePosLists);

        for(int i = 0; i < SYMMETRY_COUNT; i++) {
            Collections.sort(blackPosLists.get(i));
            Collections.sort(whitePosLists.get(i));
        }

        int min = findMinSymmetry(blackPosLists, whitePosLists);

        return sha256Hex(joinPositions(blackPosLists.get(min), whitePosLists.get(min)));
    }

    private static List<List<Integer>> createSymmetryLists() {
        List<List<Integer>> posLists = new ArrayList<>();
        for(int i = 0; i < SYMMETRY_COUNT; i++) {
            posLists.add(new ArrayList<>());
        }
        return posLists;
    }

    // Fills the black/white lists for every symmetry
    private static void parseBoardStatus(
            String boardStatus,
            List<List<Integer>> blackPosLists,
            List<List<Integer>> whitePosLists
    ) {
        int i = 0;
        while(i < boardStatus.length()) {
            int p = getBoardPositionFromString(boardStatus, i);
            List<Integer> posList = getAllSymmetryPos(p);

            for(int j = 0; j < SYMMETRY_COUNT; j++) {
                if(blackPosLists.get(j).size() <= whitePosLists.get(j).size())
                    blackPosLists.get(j).add(posList.get(j));
                else whitePosLists.get(j).add(posList.get(j));
            }

            // Columns 1-9 take two characters, 10-15 take three
            i += ((p - 1) % 15 < 9) ? 2 : 3;
        }
    }

    // Both colors take one symmetry; minimizing each color on its own merges unrelated positions
    private static int findMinSymmetry(List<List<Integer>> blackPosLists, List<List<Integer>> whitePosLists) {
        int min = 0;
        for(int i = 1; i < SYMMETRY_COUNT; i++) {
            int cmp = compareList(blackPosLists.get(i), blackPosLists.get(min));
            if(cmp == 0)
                cmp = compareList(whitePosLists.get(i), whitePosLists.get(min));
            if(cmp < 0)
                min = i;
        }
        return min;
    }

    // Delimited so [1, 23] + [4] and [1, 2] + [34] differ, and no key equals an old undelimited one
    private static String joinPositions(List<Integer> blackPos, List<Integer> whitePos) {
        return joinCells(blackPos) + "/" + joinCells(whitePos);
    }

    private static String joinCells(List<Integer> cells) {
        return cells.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder result = new StringBuilder();
            for (byte b : md.digest()) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CustomException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public static boolean validBoardString(String str) {
        int i = 0;
        while(i < str.length()) {
            char charPart = str.charAt(i);
            if(isCharNotInAtoO(charPart))
                return false;

            if(str.length() <= i + 1)
                return false;
            if(isZeroDigit(str.charAt(i + 1)))
                return false;
            int digitsNum = calculateDigitsNum(str, i + 1);

            String numberPart = str.substring(i + 1, i + 1 + digitsNum);
            int tmp = Integer.parseInt(numberPart);
            if(tmp < 1 || tmp > 15)
                return false;

            i += (digitsNum + 1);
        }

        return true;
    }

    private static int compareList(List<Integer> l1, List<Integer> l2) {
        for(int i = 0; i < l1.size(); i++) {
            if(l1.get(i) > l2.get(i)) return 1;
            else if(l1.get(i) < l2.get(i)) return -1;
        }
        return 0;
    }

    private static List<Integer> getAllSymmetryPos(int p) {
        List<Integer> posList = new ArrayList<>();

        int tmp = p;
        for(int i = 0; i < 4; i++) {
            posList.add(tmp);
            tmp = rotate90(tmp);
        }

        tmp = xAxisSymmetry(p);
        for(int i = 0; i < 4; i++) {
            posList.add(tmp);
            tmp = rotate90(tmp);
        }

        return posList;
    }

    private static int rotate90(int n) {
        int x = (n - 1) / 15;
        int y = ((n % 15) == 0) ? 15 : (n % 15);

        int cx = 15 - x;
        int cy = y - 1;

        return (cy * 15) + cx;
    }

    private static int xAxisSymmetry(int n) {
        int y = ((n % 15) == 0) ? 15 : (n % 15);
        return n - (2 * y) + 16;
    }

    private static int getBoardPositionFromString(String boardStatus, int index) {
        char charPart = boardStatus.charAt(index);

        if(isCharNotInAtoO(charPart))
            throwIllegalBoardStatusException(boardStatus);

        int n = (charPart - 'a') * 15;

        if(index + 1 >= boardStatus.length())
            throwIllegalBoardStatusException(boardStatus);
        if(isZeroDigit(boardStatus.charAt(index + 1)))
            throwIllegalBoardStatusException(boardStatus);
        int digitsNum = calculateDigitsNum(boardStatus, index + 1);

        String numberPart = boardStatus.substring(index + 1, index + 1 + digitsNum);
        int tmp = Integer.parseInt(numberPart);
        if(tmp < 1 || tmp > 15)
            throwIllegalBoardStatusException(boardStatus);

        n += tmp;

        return n;
    }

    private static void throwIllegalBoardStatusException(String boardStatus) {
        if(boardStatus == null)
            throw new NullPointerException("Board status is null");
        throw new IllegalArgumentException("Invalid board status string: " + boardStatus);
    }

    private static boolean isCharNotInAtoO(char c) {
        return 'a' > c || c > 'o';
    }

    private static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isZeroDigit(char c) {
        return '1' > c || c > '9';
    }

    private static int calculateDigitsNum(String boardStatus, int startIndex) {
        int digitsNum = 0;
        while (startIndex + digitsNum < boardStatus.length()
                && isDigit(boardStatus.charAt(startIndex + digitsNum))) {
            digitsNum++;
        }
        return digitsNum;
    }

}
