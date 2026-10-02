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
import static org.mockito.Mockito.never;
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

    @Test
    void savePuzzle_ShouldRejectWritingOverASeededPosition() {
        PuzzleCache seeded = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState(ROOT_BOARD_STATE)
                .solutionLine(SOLUTION_LINE)
                .solutionDag(new byte[] {1, 2, 3})
                .build();

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(seeded));

        // "h8h9i8" is the position the solution line answers with i9
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.savePuzzle(TYPE, PUZZLE_ID, "h8h9i8", "a1")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROTECTED_SOLUTION_POSITION);
        verify(puzzleCacheRepository, never()).save(any());
    }

    @Test
    void savePuzzle_ShouldStillAcceptPositionsOffTheSolutionLine() {
        byte[] existingDagBytes = new byte[] {1, 2, 3};
        PuzzleCache seeded = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState(ROOT_BOARD_STATE)
                .solutionLine(SOLUTION_LINE)
                .solutionDag(existingDagBytes)
                .build();

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(seeded));
        when(solutionSerializer.deserialize(existingDagBytes)).thenReturn(new HashMap<>());
        when(solutionSerializer.serialize(anyMap())).thenReturn(new byte[] {9});

        // l5 is not played anywhere in the solution line, so this is a branch the admin may fill in
        puzzleCacheService.savePuzzle(TYPE, PUZZLE_ID, "h8h9l5", "a1");

        verify(puzzleCacheRepository).save(any());
    }

    // ========== seedSolutionPath ==========

    private static final String ROOT_BOARD_STATE = "h8h9";
    // i8 by the user, i9 by the AI, j8 by the user, j9 by the AI, k8 by the user
    private static final String SOLUTION_LINE = "i8i9j8j9k8";
    private static final int CELL_I9 = 128;
    private static final int CELL_J9 = 143;

    @SuppressWarnings("unchecked")
    @Test
    void seedSolutionPath_ShouldCacheEveryAiReplyAndSkipTheTrailingUserMove() {
        byte[] serialized = new byte[] {9, 8, 7};
        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.empty());
        when(solutionSerializer.serialize(anyMap())).thenReturn(serialized);

        puzzleCacheService.seedSolutionPath(TYPE, PUZZLE_ID, ROOT_BOARD_STATE, SOLUTION_LINE);

        ArgumentCaptor<Map<Long, Integer>> pathCaptor = ArgumentCaptor.forClass(Map.class);
        verify(solutionSerializer).serialize(pathCaptor.capture());

        // keys are re-hashed from the full board string, independently of the incremental hashing
        assertThat(pathCaptor.getValue())
                .hasSize(2)
                .containsEntry(ZobristHashUtils.hashFromBoardStatus("h8h9i8"), CELL_I9)
                .containsEntry(ZobristHashUtils.hashFromBoardStatus("h8h9i8i9j8"), CELL_J9);
    }

    @Test
    void seedSolutionPath_ShouldStoreRootBoardStateAndSolutionLine() {
        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.empty());
        when(solutionSerializer.serialize(anyMap())).thenReturn(new byte[] {9});

        puzzleCacheService.seedSolutionPath(TYPE, PUZZLE_ID, ROOT_BOARD_STATE, SOLUTION_LINE);

        ArgumentCaptor<PuzzleCache> cacheCaptor = ArgumentCaptor.forClass(PuzzleCache.class);
        verify(puzzleCacheRepository).save(cacheCaptor.capture());

        PuzzleCache saved = cacheCaptor.getValue();
        assertThat(saved.getPuzzleType()).isEqualTo(TYPE);
        assertThat(saved.getPuzzleId()).isEqualTo(PUZZLE_ID);
        assertThat(saved.getRootBoardState()).isEqualTo(ROOT_BOARD_STATE);
        assertThat(saved.getSolutionLine()).isEqualTo(SOLUTION_LINE);
    }

    @SuppressWarnings("unchecked")
    @Test
    void seedSolutionPath_ShouldReplaceExistingDag_SoStaleEntriesDoNotSurvive() {
        PuzzleCache existing = PuzzleCache.builder()
                .puzzleType(TYPE).puzzleId(PUZZLE_ID)
                .rootBoardState("a1a2")
                .solutionDag(new byte[] {1, 2, 3})
                .build();

        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.of(existing));
        when(solutionSerializer.serialize(anyMap())).thenReturn(new byte[] {9});

        puzzleCacheService.seedSolutionPath(TYPE, PUZZLE_ID, ROOT_BOARD_STATE, SOLUTION_LINE);

        ArgumentCaptor<Map<Long, Integer>> pathCaptor = ArgumentCaptor.forClass(Map.class);
        verify(solutionSerializer).serialize(pathCaptor.capture());
        assertThat(pathCaptor.getValue()).hasSize(2);

        ArgumentCaptor<PuzzleCache> cacheCaptor = ArgumentCaptor.forClass(PuzzleCache.class);
        verify(puzzleCacheRepository).save(cacheCaptor.capture());
        assertThat(cacheCaptor.getValue().getRootBoardState()).isEqualTo(ROOT_BOARD_STATE);
    }

    @Test
    void seedSolutionPath_ShouldNotCreateEntry_WhenSolutionHasNoAiReply() {
        when(puzzleCacheRepository.findByPuzzleTypeAndPuzzleId(TYPE, PUZZLE_ID)).thenReturn(Optional.empty());

        puzzleCacheService.seedSolutionPath(TYPE, PUZZLE_ID, ROOT_BOARD_STATE, "i8");

        verify(puzzleCacheRepository, never()).save(any());
        verify(solutionSerializer, never()).serialize(anyMap());
    }

    @Test
    void seedSolutionPath_ShouldThrow_WhenRootBoardStateIsBlank() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.seedSolutionPath(TYPE, PUZZLE_ID, "  ", SOLUTION_LINE)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NO_BOARD_STATUS);
    }

    @Test
    void seedSolutionPath_ShouldThrow_WhenSolutionLineIsMalformed() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.seedSolutionPath(TYPE, PUZZLE_ID, ROOT_BOARD_STATE, "z9")
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
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
