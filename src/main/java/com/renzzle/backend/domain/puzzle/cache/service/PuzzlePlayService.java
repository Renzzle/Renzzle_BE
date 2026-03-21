package com.renzzle.backend.domain.puzzle.cache.service;

import com.renzzle.backend.domain.puzzle.cache.domain.Puzzle;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.domain.SolutionSerializer;
import com.renzzle.backend.domain.puzzle.cache.dao.PuzzleRepository;
import com.renzzle.backend.domain.puzzle.shared.util.ZobristHashUtils;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PuzzlePlayService {

    private static final int BOARD_SIZE = 15;
    private static final Pattern POSITION_PATTERN = Pattern.compile("^[a-o]([1-9]|1[0-5])$");

    private final PuzzleRepository puzzleRepository;
    private final SolutionSerializer solutionSerializer;

    @Transactional
    public void savePuzzle(PuzzleType puzzleType, Long puzzleId, String currentBoardState, String answerPuzzle) {
        if (puzzleType == null || puzzleId == null || currentBoardState == null || currentBoardState.isBlank()) {
            throw new CustomException(ErrorCode.NO_BOARD_STATUS);
        }

        int nextMove = parseAnswerPuzzleToMove(answerPuzzle);

        long zobristHash = ZobristHashUtils.hashFromBoardStatus(currentBoardState);

        Puzzle puzzle = puzzleRepository.findByPuzzleTypeAndSourceId(puzzleType, puzzleId)
                .orElseGet(() -> Puzzle.builder()
                        .puzzleType(puzzleType)
                        .sourceId(puzzleId)
                        .rootBoardState(currentBoardState)
                        .build());

        Map<Long, Integer> solutionDag;
        byte[] existingDag = puzzle.getSolutionDag();
        if (existingDag != null && existingDag.length > 0) {
            solutionDag = new HashMap<>(solutionSerializer.deserialize(existingDag));
        } else {
            solutionDag = new HashMap<>();
        }

        solutionDag.put(zobristHash, nextMove);

        byte[] serializedDag = solutionSerializer.serialize(solutionDag);
        Puzzle updated = puzzle.toBuilder().solutionDag(serializedDag).build();
        puzzleRepository.save(updated);
    }

    @Transactional(readOnly = true)
    public Integer getAiResponse(PuzzleType puzzleType, Long puzzleId, String currentBoardState) {
        if (puzzleType == null || puzzleId == null || currentBoardState == null || currentBoardState.isBlank()) {
            throw new CustomException(ErrorCode.NO_BOARD_STATUS);
        }

        Puzzle puzzle = puzzleRepository.findByPuzzleTypeAndSourceId(puzzleType, puzzleId)
                .orElse(null);

        if (puzzle == null) {
            return null;
        }

        final long currentZobristHash;
        try {
            currentZobristHash = ZobristHashUtils.hashFromBoardStatus(currentBoardState);
        } catch (IllegalArgumentException e) {
            throw new CustomException(e.getMessage(), ErrorCode.VALIDATION_ERROR);
        }

        Map<Long, Integer> solutionDag = solutionSerializer.deserialize(puzzle.getSolutionDag());
        return solutionDag.get(currentZobristHash);
    }

    private int parseAnswerPuzzleToMove(String answerPuzzle) {
        if (answerPuzzle == null || !POSITION_PATTERN.matcher(answerPuzzle).matches()) {
            throw new CustomException(ErrorCode.INVALID_ANSWER_POSITION);
        }
        char letter = answerPuzzle.charAt(0);
        int number = Integer.parseInt(answerPuzzle.substring(1));
        return (letter - 'a') * BOARD_SIZE + (number - 1);
    }
}
