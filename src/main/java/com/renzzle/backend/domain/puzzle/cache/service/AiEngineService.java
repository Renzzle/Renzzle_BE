package com.renzzle.backend.domain.puzzle.cache.service;

import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiEngineService {

    private final String aiEnginePath;
    private static final int TIMEOUT_SECONDS = 15;

    public AiEngineService(@Value("${ai.engine.path:/usr/local/bin/renzzle_ai_engine}") String aiEnginePath) {
        this.aiEnginePath = aiEnginePath;
    }

    public int getNextMoveFromEngine(String boardState) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(aiEnginePath, boardState);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new CustomException(ErrorCode.AI_ENGINE_ERROR);
            }

            if (output == null || output.isBlank()) {
                throw new CustomException(ErrorCode.AI_ENGINE_ERROR);
            }

            return Integer.parseInt(output.trim());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 엔진 실행 실패 (기보: {})", boardState, e);
            throw new CustomException(ErrorCode.AI_ENGINE_ERROR);
        }
    }
}
