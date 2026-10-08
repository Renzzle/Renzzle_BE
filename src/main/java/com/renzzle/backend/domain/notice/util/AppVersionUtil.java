package com.renzzle.backend.domain.notice.util;

import java.math.BigInteger;

public class AppVersionUtil {

    // Surrounding whitespace is allowed because callers trim before comparing
    public static final String VERSION_REGEX = "^\\s*\\d+(\\.\\d+)*\\s*$";

    private AppVersionUtil() {}

    // Missing trailing segments count as 0, so 1.2 equals 1.2.0
    public static int compare(String version, String other) {
        String[] left = version.trim().split("\\.");
        String[] right = other.trim().split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            BigInteger a = i < left.length ? new BigInteger(left[i]) : BigInteger.ZERO;
            BigInteger b = i < right.length ? new BigInteger(right[i]) : BigInteger.ZERO;
            int result = a.compareTo(b);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

}
