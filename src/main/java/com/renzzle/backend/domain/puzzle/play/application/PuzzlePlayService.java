package com.renzzle.backend.domain.puzzle.play.application;

import com.renzzle.backend.domain.puzzle.play.domain.Puzzle;
import com.renzzle.backend.domain.puzzle.play.domain.SolutionSerializer;
import com.renzzle.backend.domain.puzzle.play.infrastructure.PuzzleRepository;
import com.renzzle.backend.domain.puzzle.shared.util.ZobristHashUtils;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PuzzlePlayService {

    private final PuzzleRepository puzzleRepository;
    private final SolutionSerializer solutionSerializer;

    @Transactional
    public Long savePuzzle(String rootBoardState, Map<Long, Integer> solutionDag) {
        if (rootBoardState == null || rootBoardState.isBlank()) {
            throw new CustomException("rootBoardState는 비어 있을 수 없습니다.", ErrorCode.VALIDATION_ERROR);
        }

        byte[] serializedDag = solutionSerializer.serialize(solutionDag);
        Puzzle puzzle = Puzzle.builder()

                .rootBoardState(rootBoardState)
                .solutionDag(serializedDag)
                .build();

        return puzzleRepository.save(puzzle).getId();
    }

    @Transactional(readOnly = true)
    public int getAiResponse(Long puzzleId, String currentBoardState) {
        if (puzzleId == null || currentBoardState == null || currentBoardState.isBlank()) {
            throw new CustomException(ErrorCode.VALIDATION_ERROR);
        }

        Puzzle puzzle = puzzleRepository.findById(puzzleId)
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_PUZZLE));

        final long currentZobristHash;
        try {
            currentZobristHash = ZobristHashUtils.hashFromBoardStatus(currentBoardState);
        } catch (IllegalArgumentException e) {
            throw new CustomException(e.getMessage(), ErrorCode.VALIDATION_ERROR);
        }

        Map<Long, Integer> solutionDag = solutionSerializer.deserialize(puzzle.getSolutionDag());
        Integer aiResponse = solutionDag.get(currentZobristHash);

        if (aiResponse == null) {
            throw new CustomException(ErrorCode.WRONG_MOVE);
        }
        return aiResponse;
    }
}
