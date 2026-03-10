package com.renzzle.backend.domain.puzzle.play.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzzle.backend.domain.puzzle.play.domain.SolutionSerializer;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Service
public class MessagePackSolutionSerializer implements SolutionSerializer {

    private static final int MIN_MOVE_INDEX = 0;
    private static final int MAX_MOVE_INDEX = 224;
    private static final TypeReference<Map<Long, Integer>> SOLUTION_DAG_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory());

    @Override
    public byte[] serialize(Map<Long, Integer> solutionDag) {
        validateSolutionDag(solutionDag);

        try {
            return objectMapper.writeValueAsBytes(solutionDag);
        } catch (IOException e) {
            throw new CustomException("solution_dag MessagePack 직렬화에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Map<Long, Integer> deserialize(byte[] serializedSolutionDag) {
        if (serializedSolutionDag == null) {
            throw new CustomException("solution_dag 데이터는 null일 수 없습니다.", ErrorCode.VALIDATION_ERROR);
        }
        if (serializedSolutionDag.length == 0) {
            return Collections.emptyMap();
        }

        try {
            Map<Long, Integer> restored = objectMapper.readValue(serializedSolutionDag, SOLUTION_DAG_TYPE);
            validateSolutionDag(restored);
            return restored;
        } catch (IOException e) {
            throw new CustomException("solution_dag MessagePack 역직렬화에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateSolutionDag(Map<Long, Integer> solutionDag) {
        if (solutionDag == null) {
            throw new CustomException("solution_dag은 null일 수 없습니다.", ErrorCode.VALIDATION_ERROR);
        }

        for (Map.Entry<Long, Integer> entry : solutionDag.entrySet()) {
            if (entry.getKey() == null) {
                throw new CustomException("Zobrist Hash key는 null일 수 없습니다.", ErrorCode.VALIDATION_ERROR);
            }
            if (entry.getValue() == null) {
                throw new CustomException("AI 응답 수는 null일 수 없습니다.", ErrorCode.VALIDATION_ERROR);
            }
            if (entry.getValue() < MIN_MOVE_INDEX || entry.getValue() > MAX_MOVE_INDEX) {
                throw new CustomException(
                        "AI 응답 수는 0~224 범위여야 합니다. value=" + entry.getValue(),
                        ErrorCode.VALIDATION_ERROR
                );
            }
        }
    }
}
