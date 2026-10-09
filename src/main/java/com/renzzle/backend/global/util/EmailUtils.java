package com.renzzle.backend.global.util;

import java.util.Locale;

public class EmailUtils {

    private EmailUtils() {}

    // One mailbox must map to one key, or each case variant gets its own login and verification limits
    public static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

}
