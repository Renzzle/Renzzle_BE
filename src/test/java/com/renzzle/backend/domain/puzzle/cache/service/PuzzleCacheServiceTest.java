package com.renzzle.backend.domain.puzzle.cache.service;

import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleCache;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.domain.SolutionSerializer;
import com.renzzle.backend.domain.puzzle.cache.dao.PuzzleCacheRepository;
import com.renzzle.backend.domain.puzzle.shared.util.ZobristHashUtils;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuzzleCacheServiceTest {

    @Mock
    private PuzzleCacheRepository puzzleCacheRepository;

    @Mock
    private SolutionSerializer solutionSerializer;

    @InjectMocks
    private PuzzleCacheService puzzleCacheService;

    private static final PuzzleType TYPE = PuzzleType.COMMUNITY;
    private static final Long PUZZLE_ID = 10L;

    // ========== getAiResponse ==========

    @Test
    void getAiResponse_ShouldReturnAnswer_WhenHashExists() {
        String currentBoardState = "h8h9";
        Long zobristHash = ZobristHashUtils.hashFromBoardStatus(currentBoardState);
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        PuzzleCache puzzle = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState("B...W...")
                .solutionDag(solutionDagBinary)
                .build();

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(solutionDagBinary)).thenReturn(Map.of(zobristHash, 113));

        Integer aiResponse = puzzleCacheService.getAiResponse(TYPE, PUZZLE_ID, currentBoardState);

        assertThat(aiResponse).isEqualTo(113);
    }

    @Test
    void getAiResponse_ShouldReturnNull_WhenHashDoesNotExist() {
        String currentBoardState = "h8h10";
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        PuzzleCache puzzle = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState("B...W...")
                .solutionDag(solutionDagBinary)
                .build();

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(solutionDagBinary)).thenReturn(Map.of(300L, 44));

        Integer aiResponse = puzzleCacheService.getAiResponse(TYPE, PUZZLE_ID, currentBoardState);

        assertThat(aiResponse).isNull();
    }

    @Test
    void getAiResponse_ShouldReturnNull_WhenPuzzleNotFound() {
        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, 999L)).thenReturn(Optional.empty());

        Integer aiResponse = puzzleCacheService.getAiResponse(TYPE, 999L, "h8");

        assertThat(aiResponse).isNull();
    }

    @Test
    void getAiResponse_ShouldThrowValidation_WhenBoardStateInvalid() {
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        PuzzleCache puzzle = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState("B...W...")
                .solutionDag(solutionDagBinary)
                .build();

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(puzzle));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.getAiResponse(TYPE, PUZZLE_ID, "z99")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    // ========== savePuzzle ==========

    @Test
    void savePuzzle_ShouldSerializeAndPersist_WhenCacheExists() {
        String currentBoardState = "h8h9";
        String answerPuzzle = "h8";
        byte[] existingDag = new byte[] {1, 2, 3};
        byte[] serialized = new byte[] {9, 8, 7};

        PuzzleCache puzzle = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState("B...W...")
                .solutionDag(existingDag)
                .build();

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(existingDag)).thenReturn(Map.of());
        when(solutionSerializer.serialize(anyMap())).thenReturn(serialized);

        puzzleCacheService.savePuzzle(TYPE, PUZZLE_ID, currentBoardState, answerPuzzle);

        verify(puzzleCacheRepository).save(any(PuzzleCache.class));
    }

    @Test
    void savePuzzle_ShouldCreateNewCache_WhenNotExists() {
        String currentBoardState = "h8h9";
        String answerPuzzle = "h8";
        byte[] serialized = new byte[] {9, 8, 7};

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.empty());
        when(solutionSerializer.serialize(anyMap())).thenReturn(serialized);

        puzzleCacheService.savePuzzle(TYPE, PUZZLE_ID, currentBoardState, answerPuzzle);

        ArgumentCaptor<PuzzleCache> puzzleCaptor = ArgumentCaptor.forClass(PuzzleCache.class);
        verify(puzzleCacheRepository).save(puzzleCaptor.capture());

        PuzzleCache saved = puzzleCaptor.getValue();
        assertThat(saved.getPuzzleType()).isEqualTo(TYPE);
        assertThat(saved.getPuzzleId()).isEqualTo(PUZZLE_ID);
    }

    @Test
    void savePuzzle_ShouldThrowInvalidAnswerPosition_WhenLetterOutOfRange() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.savePuzzle(TYPE, PUZZLE_ID, "h8h9", "z7")
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ANSWER_POSITION);
    }

    @Test
    void savePuzzle_ShouldThrowInvalidAnswerPosition_WhenNumberOutOfRange() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.savePuzzle(TYPE, PUZZLE_ID, "h8h9", "a16")
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ANSWER_POSITION);
    }

    @Test
    void savePuzzle_ShouldThrowInvalidAnswerPosition_WhenNumberIsZero() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.savePuzzle(TYPE, PUZZLE_ID, "h8h9", "a0")
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ANSWER_POSITION);
    }

    @SuppressWarnings("unchecked")
    @Test
    void savePuzzle_ShouldMergeNewEntryIntoExistingDag() {
        String currentBoardState = "h8h9";
        String answerPuzzle = "h8";
        long zobristHash = ZobristHashUtils.hashFromBoardStatus(currentBoardState);
        int newMove = 112;
        long existingHash = 999L;
        int existingMove = 50;

        byte[] existingDagBytes = new byte[] {1, 2, 3};
        byte[] serialized = new byte[] {9, 8, 7};

        Map<Long, Integer> existingDag = new HashMap<>();
        existingDag.put(existingHash, existingMove);

        PuzzleCache puzzle = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState("B...W...")
                .solutionDag(existingDagBytes)
                .build();

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(existingDagBytes)).thenReturn(existingDag);
        when(solutionSerializer.serialize(anyMap())).thenReturn(serialized);

        puzzleCacheService.savePuzzle(TYPE, PUZZLE_ID, currentBoardState, answerPuzzle);

        ArgumentCaptor<Map<Long, Integer>> dagCaptor = ArgumentCaptor.forClass(Map.class);
        verify(solutionSerializer).serialize(dagCaptor.capture());

        Map<Long, Integer> mergedDag = dagCaptor.getValue();
        assertThat(mergedDag)
                .hasSize(2)
                .containsEntry(existingHash, existingMove)
                .containsEntry(zobristHash, newMove);
    }

    // ========== getNextMoveCandidates ==========

    private static final String USER_TURN_BOARD_STATE = "h8h9";
    private static final int USER_MOVE_INDEX = 2;
    private static final int CELL_I10 = 129;
    private static final int CELL_J11 = 145;
    private static final int CELL_H8 = 112;

    private static long hashAfterUserMove(int cellIndex) {
        return ZobristHashUtils.applyMove(
                ZobristHashUtils.hashFromBoardStatus(USER_TURN_BOARD_STATE), cellIndex, USER_MOVE_INDEX);
    }

    @Test
    void getNextMoveCandidates_ShouldReturnOnlyCachedMoves() {
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        PuzzleCache puzzle = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState(USER_TURN_BOARD_STATE)
                .solutionDag(solutionDagBinary)
                .build();

        Map<Long, Integer> dag = new HashMap<>();
        dag.put(hashAfterUserMove(CELL_I10), CELL_J11);
        dag.put(12345L, 7); // unreachable entry, so it must not appear among the candidates

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(solutionDagBinary)).thenReturn(dag);

        Map<Integer, Integer> candidates =
                puzzleCacheService.getNextMoveCandidates(TYPE, PUZZLE_ID, USER_TURN_BOARD_STATE);

        assertThat(candidates).containsExactlyEntriesOf(Map.of(CELL_I10, CELL_J11));
    }

    @Test
    void getNextMoveCandidates_ShouldSkipOccupiedCells() {
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        PuzzleCache puzzle = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState(USER_TURN_BOARD_STATE)
                .solutionDag(solutionDagBinary)
                .build();

        Map<Long, Integer> dag = new HashMap<>();
        dag.put(hashAfterUserMove(CELL_H8), CELL_J11); // h8 is already occupied

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(solutionDagBinary)).thenReturn(dag);

        Map<Integer, Integer> candidates =
                puzzleCacheService.getNextMoveCandidates(TYPE, PUZZLE_ID, USER_TURN_BOARD_STATE);

        assertThat(candidates).isEmpty();
    }

    @Test
    void getNextMoveCandidates_ShouldReturnEmpty_WhenPuzzleNotFound() {
        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, 999L)).thenReturn(Optional.empty());

        Map<Integer, Integer> candidates =
                puzzleCacheService.getNextMoveCandidates(TYPE, 999L, USER_TURN_BOARD_STATE);

        assertThat(candidates).isEmpty();
    }

    @Test
    void getNextMoveCandidates_ShouldReturnEmpty_WhenSolutionDagIsEmpty() {
        PuzzleCache puzzle = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState(USER_TURN_BOARD_STATE)
                .solutionDag(new byte[0])
                .build();

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(puzzle));

        Map<Integer, Integer> candidates =
                puzzleCacheService.getNextMoveCandidates(TYPE, PUZZLE_ID, USER_TURN_BOARD_STATE);

        assertThat(candidates).isEmpty();
    }

    @Test
    void getNextMoveCandidates_ShouldThrowValidation_WhenBoardStateInvalid() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.getNextMoveCandidates(TYPE, PUZZLE_ID, "z99")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void getNextMoveCandidates_ShouldThrowNoBoardStatus_WhenBoardStateBlank() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.getNextMoveCandidates(TYPE, PUZZLE_ID, "  ")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_BOARD_STATUS);
    }
}
