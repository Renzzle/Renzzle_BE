package com.renzzle.backend.domain.puzzle.cache.service;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisabledOnOs(OS.WINDOWS)
class AiEngineServiceTest {

    @Test
    @DisplayName("정상 출력이면 int 값을 반환한다")
    void shouldReturnMove_WhenEngineOutputsValidInt() {
        AiEngineService service = new AiEngineService("/bin/sh");
        int result = service.getNextMoveFromEngine("-c echo 112");
        assertThat(result).isEqualTo(112);
    }

    @Test
    @DisplayName("존재하지 않는 실행 파일이면 AI_ENGINE_ERROR가 발생한다")
    void shouldThrowAiEngineError_WhenPathDoesNotExist() {
        AiEngineService service = new AiEngineService("/nonexistent/binary");

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.getNextMoveFromEngine("h8h9")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_ENGINE_ERROR);
    }

    @Test
    @DisplayName("빈 출력이면 AI_ENGINE_ERROR가 발생한다")
    void shouldThrowAiEngineError_WhenOutputIsEmpty() {
        AiEngineService service = new AiEngineService("/bin/sh");

        CustomException exception = assertThrows(
                CustomException.class,
                () -> service.getNextMoveFromEngine("-c echo")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_ENGINE_ERROR);
    }
}
