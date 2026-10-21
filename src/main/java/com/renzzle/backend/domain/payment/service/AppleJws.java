package com.renzzle.backend.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;

// Reads the payload of a JWS from Apple without checking its signature. Callers either fetched it from Apple
// over TLS or only use it to know which transaction to look up again
final class AppleJws {

    private AppleJws() {
    }

    static <T> T decodePayload(String jws, ObjectMapper objectMapper, Class<T> type) {
        if (jws == null) {
            throw new IllegalArgumentException("Missing JWS");
        }
        String[] parts = jws.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWS format");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return objectMapper.readValue(payload, type);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to decode JWS payload", e);
        }
    }
}
