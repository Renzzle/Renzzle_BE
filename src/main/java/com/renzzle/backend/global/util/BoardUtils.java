package com.renzzle.backend.global.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BoardUtils {

    private BoardUtils() {}

    public static String makeBoardKey(String boardStatus) {
        if(boardStatus == null || boardStatus.isBlank())
            throwIllegalBoardStatusException(boardStatus);

        List<List<Integer>> blackPosLists = new ArrayList<>();
        List<List<Integer>> whitePosLists = new ArrayList<>();

        for(int i = 0; i < 8; i++) {
            blackPosLists.add(new ArrayList<>());
            whitePosLists.add(new ArrayList<>());
        }

        // parse board status string
        for(int i = 0; i < boardStatus.length();) {
            int p = getBoardPositionFromString(boardStatus, i);
            List<Integer> posList = getAllSymmetryPos(p);

            for(int j = 0; j < 8; j++) {
                if(blackPosLists.get(j).size() <= whitePosLists.get(j).size())
                    blackPosLists.get(j).add(posList.get(j));
                else whitePosLists.get(j).add(posList.get(j));
            }

            // calculate increment based on position
            i += ((p - 1) % 15 < 9) ? 2 : 3;
        }

        // sort all lists
        for(int i = 0; i < 8; i++) {
            Collections.sort(blackPosLists.get(i));
            Collections.sort(whitePosLists.get(i));
        }

        // find minimum value list
        List<Integer> minB = blackPosLists.get(0);
        List<Integer> minW = whitePosLists.get(0);
        for(int i = 1; i < 8; i++) {
            if(compareList(minB, blackPosLists.get(i)) > 0) {
                minB = blackPosLists.get(i);
            }
            if(compareList(minW, whitePosLists.get(i)) > 0) {
                minW = whitePosLists.get(i);
            }
        }

        // make string key
        StringBuilder result = new StringBuilder();

        for (Integer num : minB) {
            char c = (char) (num + 32);
            result.append(c);
        }
        for (Integer num : minW) {
            char c = (char) (num + 32);
            result.append(c);
        }

        return result.toString();
    }

    public static boolean validBoardString(String str) {
        for(int i = 0; i < str.length();) {
            char charPart = str.charAt(i);
            if(!isCharInAtoO(charPart))
                return false;

            if(!isNonZeroDigit(str.charAt(i + 1)))
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

        if(!isCharInAtoO(charPart))
            throwIllegalBoardStatusException(boardStatus);

        int n = (charPart - 'a') * 15;

        // determine the number of digits
        if(!isNonZeroDigit(boardStatus.charAt(index + 1)))
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

    private static boolean isCharInAtoO(char c) {
        return 'a' <= c && c <= 'o';
    }

    private static boolean isDigit(char c) {
        return '0' <= c && c <= '9';
    }

    private static boolean isNonZeroDigit(char c) {
        return '1' <= c && c <= '9';
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
