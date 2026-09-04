package com.renzzle.backend.domain.puzzle.cache.dao;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessagePackSolutionSerializerTest {

    private final MessagePackSolutionSerializer serializer = new MessagePackSolutionSerializer();

    @Test
    void serializeAndDeserialize_ShouldRoundTrip() {
        // given
        Map<Long, Integer> dag = Map.of(
                1442282180497L, 0,
                1442282180498L, 122,
                1442282180499L, 224
        );

        // when
        byte[] serialized = serializer.serialize(dag);
        Map<Long, Integer> restored = serializer.deserialize(serialized);

        // then
        assertThat(serialized).isNotEmpty();
        assertThat(restored).containsExactlyInAnyOrderEntriesOf(dag);
    }

    @Test
    void serialize_ShouldThrow_WhenMoveOutOfRange() {
        // given
        Map<Long, Integer> invalidDag = Map.of(1L, 225);

        // when
        CustomException exception = assertThrows(CustomException.class, () -> serializer.serialize(invalidDag));

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
}
