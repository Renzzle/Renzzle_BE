package com.renzzle.backend.domain.puzzle.community.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

public final class SharedMovesEncoder {

    private static final String BASE64_URL_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    // The share page decodes with this same alphabet
    private static final String SHUFFLED_ALPHABET =
            "WyR9_QfXoZ7S1kBUMiOes4dacthv0rj3gYVJz5GAmNEIwCD-682TbnHqLFuxpKlP";
    private static final char[] SUBSTITUTION = buildSubstitution();

    private SharedMovesEncoder() {
    }

    // Byte pairs (user cell, AI cell), XORed with SHA-256(token + board) chained, then base64url in a shuffled alphabet
    public static String encode(Map<Integer, Integer> candidates, String token, String boardState) {
        byte[] bytes = new byte[candidates.size() * 2];
        int i = 0;
        for (Map.Entry<Integer, Integer> candidate : candidates.entrySet()) {
            bytes[i++] = candidate.getKey().byteValue();
            bytes[i++] = candidate.getValue().byteValue();
        }

        byte[] keystream = keystream(token + boardState, bytes.length);
        for (int j = 0; j < bytes.length; j++) {
            bytes[j] ^= keystream[j];
        }

        char[] encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).toCharArray();
        for (int j = 0; j < encoded.length; j++) {
            encoded[j] = SUBSTITUTION[encoded[j]];
        }
        return new String(encoded);
    }

    private static byte[] keystream(String seed, int length) {
        MessageDigest sha256;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }

        byte[] keystream = new byte[length];
        byte[] block = sha256.digest(seed.getBytes(StandardCharsets.UTF_8));
        for (int filled = 0; filled < length; filled += block.length) {
            System.arraycopy(block, 0, keystream, filled, Math.min(block.length, length - filled));
            block = sha256.digest(block);
        }
        return keystream;
    }

    private static char[] buildSubstitution() {
        char[] substitution = new char[128];
        for (int i = 0; i < BASE64_URL_ALPHABET.length(); i++) {
            substitution[BASE64_URL_ALPHABET.charAt(i)] = SHUFFLED_ALPHABET.charAt(i);
        }
        return substitution;
    }
}
