package com.renzzle.backend.domain.puzzle.cache.service;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleCache;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.domain.SolutionSerializer;
import com.renzzle.backend.domain.puzzle.cache.dao.PuzzleCacheRepository;
import com.renzzle.backend.domain.puzzle.shared.util.ZobristHashUtils;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PuzzleCacheService {

    private static final int BOARD_SIZE = 15;
    private static final Pattern POSITION_PATTERN = Pattern.compile("^[a-o]([1-9]|1[0-5])$");

    private final PuzzleCacheRepository puzzleCacheRepository;
    private final SolutionSerializer solutionSerializer;

    @Async
    @Transactional
    public void savePuzzle(PuzzleType puzzleType, Long puzzleId, String currentBoardState, String answerPuzzle) {
        if (puzzleType == null || puzzleId == null || currentBoardState == null || currentBoardState.isBlank()) {
            throw new CustomException(ErrorCode.NO_BOARD_STATUS);
        }

        int nextMove = parseAnswerPuzzleToMove(answerPuzzle);

        long zobristHash = ZobristHashUtils.hashFromBoardStatus(currentBoardState);

        PuzzleCache puzzle = puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(puzzleType, puzzleId)
                .orElseGet(() -> PuzzleCache.builder()
                        .puzzleType(puzzleType)
                        .puzzleId(puzzleId)
                        .rootBoardState(currentBoardState)
                        .build());

        if (isSeededPosition(puzzle, zobristHash)) {
            throw new CustomException(ErrorCode.PROTECTED_SOLUTION_POSITION);
        }

        Map<Long, Integer> solutionDag;
        byte[] existingDag = puzzle.getSolutionDag();
        if (existingDag != null && existingDag.length > 0) {
            solutionDag = new HashMap<>(solutionSerializer.deserialize(existingDag));
        } else {
            solutionDag = new HashMap<>();
        }

        solutionDag.put(zobristHash, nextMove);

        byte[] serializedDag = solutionSerializer.serialize(solutionDag);
        PuzzleCache updated = puzzle.toBuilder().solutionDag(serializedDag).build();
        puzzleCacheRepository.save(updated);
    }

    // Keys only AI-to-move positions; replacing the DAG clears entries from old edits
    @Transactional
    public void seedSolutionPath(PuzzleType puzzleType, Long puzzleId, String rootBoardState, String answer) {
        if (puzzleType == null || puzzleId == null || rootBoardState == null || rootBoardState.isBlank()) {
            throw new CustomException(ErrorCode.NO_BOARD_STATUS);
        }

        Map<Long, Integer> solutionPath = buildSolutionPath(rootBoardState, answer);

        PuzzleCache puzzle = puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(puzzleType, puzzleId)
                .orElse(null);
        if (puzzle == null) {
            if (solutionPath.isEmpty()) {
                return;
            }
            puzzle = PuzzleCache.builder()
                    .puzzleType(puzzleType)
                    .puzzleId(puzzleId)
                    .rootBoardState(rootBoardState)
                    .build();
        }

        PuzzleCache seeded = puzzle.toBuilder()
                .rootBoardState(rootBoardState)
                .solutionLine(answer)
                .solutionDag(solutionSerializer.serialize(solutionPath))
                .build();
        puzzleCacheRepository.save(seeded);
    }

    private boolean isSeededPosition(PuzzleCache puzzle, long zobristHash) {
        String solutionLine = puzzle.getSolutionLine();
        if (solutionLine == null || solutionLine.isBlank()) {
            return false;
        }
        return buildSolutionPath(puzzle.getRootBoardState(), solutionLine).containsKey(zobristHash);
    }

    private Map<Long, Integer> buildSolutionPath(String rootBoardState, String answer) {
        final int[] rootCells;
        final int[] answerCells;
        try {
            rootCells = ZobristHashUtils.parseCellIndexes(rootBoardState);
            answerCells = ZobristHashUtils.parseCellIndexes(answer);
        } catch (IllegalArgumentException e) {
            throw new CustomException(e.getMessage(), ErrorCode.VALIDATION_ERROR);
        }

        Map<Long, Integer> solutionPath = new HashMap<>();
        long hash = ZobristHashUtils.hashFromCellIndexes(rootCells);
        int moveIndex = rootCells.length;
        for (int i = 0; i + 1 < answerCells.length; i += 2) {
            hash = ZobristHashUtils.applyMove(hash, answerCells[i], moveIndex++);
            solutionPath.put(hash, answerCells[i + 1]);
            hash = ZobristHashUtils.applyMove(hash, answerCells[i + 1], moveIndex++);
        }
        return solutionPath;
    }

    @Transactional(readOnly = true)
    public Integer getAiResponse(PuzzleType puzzleType, Long puzzleId, String currentBoardState) {
        if (puzzleType == null || puzzleId == null || currentBoardState == null || currentBoardState.isBlank()) {
            throw new CustomException(ErrorCode.NO_BOARD_STATUS);
        }

        PuzzleCache puzzle = puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(puzzleType, puzzleId)
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

    // Takes a user-to-move position; maps each cached user move to the AI reply (0-based cells)
    @Transactional(readOnly = true)
    public Map<Integer, Integer> getNextMoveCandidates(PuzzleType puzzleType, Long puzzleId, String currentBoardState) {
        if (puzzleType == null || puzzleId == null || currentBoardState == null || currentBoardState.isBlank()) {
            throw new CustomException(ErrorCode.NO_BOARD_STATUS);
        }

        final int[] currentCellIndexes;
        try {
            currentCellIndexes = ZobristHashUtils.parseCellIndexes(currentBoardState);
        } catch (IllegalArgumentException e) {
            throw new CustomException(e.getMessage(), ErrorCode.VALIDATION_ERROR);
        }

        PuzzleCache puzzle = puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(puzzleType, puzzleId)
                .orElse(null);

        if (puzzle == null || puzzle.getSolutionDag() == null || puzzle.getSolutionDag().length == 0) {
            return Map.of();
        }

        Map<Long, Integer> solutionDag = solutionSerializer.deserialize(puzzle.getSolutionDag());
        if (solutionDag.isEmpty()) {
            return Map.of();
        }

        int totalCells = ZobristHashUtils.totalCells();
        boolean[] occupied = new boolean[totalCells];
        for (int cellIndex : currentCellIndexes) {
            occupied[cellIndex] = true;
        }

        long currentZobristHash = ZobristHashUtils.hashFromCellIndexes(currentCellIndexes);
        int userMoveIndex = currentCellIndexes.length;

        Map<Integer, Integer> candidates = new LinkedHashMap<>();
        for (int cellIndex = 0; cellIndex < totalCells; cellIndex++) {
            if (occupied[cellIndex]) {
                continue;
            }
            long nextHash = ZobristHashUtils.applyMove(currentZobristHash, cellIndex, userMoveIndex);
            Integer aiResponse = solutionDag.get(nextHash);
            if (aiResponse != null) {
                candidates.put(cellIndex, aiResponse);
            }
        }
        return candidates;
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
