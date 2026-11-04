package com.renzzle.backend.domain.puzzle.community.util;

import com.renzzle.backend.support.SharedMovesTestDecoder;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SharedMovesEncoderTest {

    private static final String TOKEN = "3f1c2b9e-8a7d-4c6b-9e5f-1a2b3c4d5e6f";
    private static final String BOARD = "h8i8h9i9h10i10";

    @Test
    void encode_WhenDecodedWithTheSameTokenAndBoard_ThenGivesBackTheCandidates() {
        Map<Integer, Integer> candidates = new LinkedHashMap<>();
        candidates.put(115, 116);
        candidates.put(224, 0);

        String encoded = SharedMovesEncoder.encode(candidates, TOKEN, BOARD);

        assertThat(SharedMovesTestDecoder.decode(encoded, TOKEN, BOARD)).isEqualTo(candidates);
    }

    @Test
    void encode_WhenLongerThanOneHashBlock_ThenStillDecodes() {
        // 20 pairs are 40 bytes, past the first 32-byte block
        Map<Integer, Integer> candidates = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            candidates.put(i * 11, 224 - i);
        }

        String encoded = SharedMovesEncoder.encode(candidates, TOKEN, BOARD);

        assertThat(SharedMovesTestDecoder.decode(encoded, TOKEN, BOARD)).isEqualTo(candidates);
    }

    @Test
    void encode_WhenNoCandidates_ThenReturnsEmpty() {
        assertThat(SharedMovesEncoder.encode(Map.of(), TOKEN, BOARD)).isEmpty();
    }

    @Test
    void encode_WhenBoardDiffers_ThenTheSameCandidatesEncodeDifferently() {
        Map<Integer, Integer> candidates = Map.of(115, 116);

        assertThat(SharedMovesEncoder.encode(candidates, TOKEN, BOARD))
                .isNotEqualTo(SharedMovesEncoder.encode(candidates, TOKEN, BOARD + "j9"));
    }

    @Test
    void encode_ThenUsesOnlyUrlSafeCharacters() {
        Map<Integer, Integer> candidates = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            candidates.put(i * 7, i);
        }

        assertThat(SharedMovesEncoder.encode(candidates, TOKEN, BOARD)).matches("[A-Za-z0-9_-]+");
    }
}
