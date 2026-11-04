package com.renzzle.backend.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

// Written apart from SharedMovesEncoder, the way the share page decodes it
public final class SharedMovesTestDecoder {

    private static final String BASE64_URL_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final String SHUFFLED_ALPHABET =
            "WyR9_QfXoZ7S1kBUMiOes4dacthv0rj3gYVJz5GAmNEIwCD-682TbnHqLFuxpKlP";

    private SharedMovesTestDecoder() {
    }

    public static Map<Integer, Integer> decode(String encoded, String token, String boardState) {
        StringBuilder base64 = new StringBuilder();
        for (char c : encoded.toCharArray()) {
            base64.append(BASE64_URL_ALPHABET.charAt(SHUFFLED_ALPHABET.indexOf(c)));
        }
        byte[] bytes = Base64.getUrlDecoder().decode(base64.toString());

        byte[] block = sha256((token + boardState).getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && i % block.length == 0) {
                block = sha256(block);
            }
            bytes[i] ^= block[i % block.length];
        }

        Map<Integer, Integer> candidates = new LinkedHashMap<>();
        for (int i = 0; i + 1 < bytes.length; i += 2) {
            candidates.put(bytes[i] & 0xFF, bytes[i + 1] & 0xFF);
        }
        return candidates;
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
