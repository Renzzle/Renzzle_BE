package com.renzzle.backend.domain.puzzle.cache.service;

import com.renzzle.backend.domain.puzzle.cache.domain.Puzzle;
import com.renzzle.backend.domain.puzzle.cache.domain.PuzzleType;
import com.renzzle.backend.domain.puzzle.cache.domain.SolutionSerializer;
import com.renzzle.backend.domain.puzzle.cache.dao.PuzzleRepository;
import com.renzzle.backend.domain.puzzle.shared.util.ZobristHashUtils;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PuzzleCacheServiceTest {

    @Mock
    private PuzzleRepository puzzleRepository;

    @Mock
    private SolutionSerializer solutionSerializer;

    @InjectMocks
    private PuzzleCacheService puzzleCacheService;

    private static final PuzzleType TYPE = PuzzleType.COMMUNITY;
    private static final Long SOURCE_ID = 10L;

    // ========== getAiResponse ==========

    @Test
    @DisplayName("현재 보드 상태를 Zobrist Hash로 변환해 AI 응답 수를 반환한다")
    void getAiResponse_ShouldReturnAnswer_WhenHashExists() {
        String currentBoardState = "h8h9";
        Long zobristHash = ZobristHashUtils.hashFromBoardStatus(currentBoardState);
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        Puzzle puzzle = Puzzle.builder()
                .puzzleType(TYPE).sourceId(SOURCE_ID)
                .rootBoardState("B...W...")
                .solutionDag(solutionDagBinary)
                .build();

        when(puzzleRepository.findByPuzzleTypeAndSourceId(TYPE, SOURCE_ID)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(solutionDagBinary)).thenReturn(Map.of(zobristHash, 113));

        Integer aiResponse = puzzleCacheService.getAiResponse(TYPE, SOURCE_ID, currentBoardState);

        assertThat(aiResponse).isEqualTo(113);
    }

    @Test
    @DisplayName("Zobrist Hash가 DAG에 없으면 null을 반환한다")
    void getAiResponse_ShouldReturnNull_WhenHashDoesNotExist() {
        String currentBoardState = "h8h10";
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        Puzzle puzzle = Puzzle.builder()
                .puzzleType(TYPE).sourceId(SOURCE_ID)
                .rootBoardState("B...W...")
                .solutionDag(solutionDagBinary)
                .build();

        when(puzzleRepository.findByPuzzleTypeAndSourceId(TYPE, SOURCE_ID)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(solutionDagBinary)).thenReturn(Map.of(300L, 44));

        Integer aiResponse = puzzleCacheService.getAiResponse(TYPE, SOURCE_ID, currentBoardState);

        assertThat(aiResponse).isNull();
    }

    @Test
    @DisplayName("캐시 퍼즐이 없으면 null을 반환한다")
    void getAiResponse_ShouldReturnNull_WhenPuzzleNotFound() {
        when(puzzleRepository.findByPuzzleTypeAndSourceId(TYPE, 999L)).thenReturn(Optional.empty());

        Integer aiResponse = puzzleCacheService.getAiResponse(TYPE, 999L, "h8");

        assertThat(aiResponse).isNull();
    }

    @Test
    @DisplayName("보드 상태 문자열이 잘못되면 VALIDATION_ERROR 예외가 발생한다")
    void getAiResponse_ShouldThrowValidation_WhenBoardStateInvalid() {
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        Puzzle puzzle = Puzzle.builder()
                .puzzleType(TYPE).sourceId(SOURCE_ID)
                .rootBoardState("B...W...")
                .solutionDag(solutionDagBinary)
                .build();

        when(puzzleRepository.findByPuzzleTypeAndSourceId(TYPE, SOURCE_ID)).thenReturn(Optional.of(puzzle));

        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.getAiResponse(TYPE, SOURCE_ID, "z99")
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    // ========== savePuzzle ==========

    @Test
    @DisplayName("기존 캐시가 있으면 DAG에 추가되어 저장된다")
    void savePuzzle_ShouldSerializeAndPersist_WhenCacheExists() {
        String currentBoardState = "h8h9";
        String answerPuzzle = "h8";
        byte[] existingDag = new byte[] {1, 2, 3};
        byte[] serialized = new byte[] {9, 8, 7};

        Puzzle puzzle = Puzzle.builder()
                .puzzleType(TYPE).sourceId(SOURCE_ID)
                .rootBoardState("B...W...")
                .solutionDag(existingDag)
                .build();

        when(puzzleRepository.findByPuzzleTypeAndSourceId(TYPE, SOURCE_ID)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(existingDag)).thenReturn(Map.of());
        when(solutionSerializer.serialize(any(Map.class))).thenReturn(serialized);

        puzzleCacheService.savePuzzle(TYPE, SOURCE_ID, currentBoardState, answerPuzzle);

        verify(puzzleRepository).save(any(Puzzle.class));
    }

    @Test
    @DisplayName("캐시가 없으면 새로 생성되어 저장된다")
    void savePuzzle_ShouldCreateNewCache_WhenNotExists() {
        String currentBoardState = "h8h9";
        String answerPuzzle = "h8";
        byte[] serialized = new byte[] {9, 8, 7};

        when(puzzleRepository.findByPuzzleTypeAndSourceId(TYPE, SOURCE_ID)).thenReturn(Optional.empty());
        when(solutionSerializer.serialize(any(Map.class))).thenReturn(serialized);

        puzzleCacheService.savePuzzle(TYPE, SOURCE_ID, currentBoardState, answerPuzzle);

        ArgumentCaptor<Puzzle> puzzleCaptor = ArgumentCaptor.forClass(Puzzle.class);
        verify(puzzleRepository).save(puzzleCaptor.capture());

        Puzzle saved = puzzleCaptor.getValue();
        assertThat(saved.getPuzzleType()).isEqualTo(TYPE);
        assertThat(saved.getSourceId()).isEqualTo(SOURCE_ID);
    }

    @Test
    @DisplayName("answerPuzzle이 범위 밖 알파벳이면 INVALID_ANSWER_POSITION 예외가 발생한다")
    void savePuzzle_ShouldThrowInvalidAnswerPosition_WhenLetterOutOfRange() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.savePuzzle(TYPE, SOURCE_ID, "h8h9", "z7")
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ANSWER_POSITION);
    }

    @Test
    @DisplayName("answerPuzzle이 범위 밖 숫자이면 INVALID_ANSWER_POSITION 예외가 발생한다")
    void savePuzzle_ShouldThrowInvalidAnswerPosition_WhenNumberOutOfRange() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.savePuzzle(TYPE, SOURCE_ID, "h8h9", "a16")
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ANSWER_POSITION);
    }

    @Test
    @DisplayName("answerPuzzle이 숫자 0이면 INVALID_ANSWER_POSITION 예외가 발생한다")
    void savePuzzle_ShouldThrowInvalidAnswerPosition_WhenNumberIsZero() {
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzleCacheService.savePuzzle(TYPE, SOURCE_ID, "h8h9", "a0")
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ANSWER_POSITION);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("기존 DAG에 새 항목이 정상적으로 merge 된다")
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

        Puzzle puzzle = Puzzle.builder()
                .puzzleType(TYPE).sourceId(SOURCE_ID)
                .rootBoardState("B...W...")
                .solutionDag(existingDagBytes)
                .build();

        when(puzzleRepository.findByPuzzleTypeAndSourceId(TYPE, SOURCE_ID)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(existingDagBytes)).thenReturn(existingDag);
        when(solutionSerializer.serialize(any(Map.class))).thenReturn(serialized);

        puzzleCacheService.savePuzzle(TYPE, SOURCE_ID, currentBoardState, answerPuzzle);

        ArgumentCaptor<Map<Long, Integer>> dagCaptor = ArgumentCaptor.forClass(Map.class);
        verify(solutionSerializer).serialize(dagCaptor.capture());

        Map<Long, Integer> mergedDag = dagCaptor.getValue();
        assertThat(mergedDag).hasSize(2);
        assertThat(mergedDag).containsEntry(existingHash, existingMove);
        assertThat(mergedDag).containsEntry(zobristHash, newMove);
    }
}
