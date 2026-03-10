package com.renzzle.backend.domain.puzzle.play.application;

import com.renzzle.backend.domain.puzzle.play.domain.Puzzle;
import com.renzzle.backend.domain.puzzle.play.domain.SolutionSerializer;
import com.renzzle.backend.domain.puzzle.play.infrastructure.PuzzleRepository;
import com.renzzle.backend.domain.puzzle.shared.util.ZobristHashUtils;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PuzzlePlayServiceTest {

    @Mock
    private PuzzleRepository puzzleRepository;

    @Mock
    private SolutionSerializer solutionSerializer;

    @InjectMocks
    private PuzzlePlayService puzzlePlayService;

    @Test
    @DisplayName("현재 보드 상태를 내부에서 Zobrist Hash로 변환해 AI 응답 수를 반환한다")
    void getAiResponse_ShouldReturnAnswer_WhenHashExists() {
        // given
        Long puzzleId = 10L;
        String currentBoardState = "h8h9";
        Long zobristHash = ZobristHashUtils.hashFromBoardStatus(currentBoardState);
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        Puzzle puzzle = Puzzle.builder()
                .id(puzzleId)
                .rootBoardState("B...W...")
                .solutionDag(solutionDagBinary)
                .build();

        when(puzzleRepository.findById(puzzleId)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(solutionDagBinary)).thenReturn(Map.of(zobristHash, 113));

        // when
        int aiResponse = puzzlePlayService.getAiResponse(puzzleId, currentBoardState);

        // then
        assertThat(aiResponse).isEqualTo(113);
    }

    @Test
    @DisplayName("현재 Zobrist Hash가 DAG에 없으면 WRONG_MOVE 예외가 발생한다")
    void getAiResponse_ShouldThrowWrongMove_WhenHashDoesNotExist() {
        // given
        Long puzzleId = 10L;
        String currentBoardState = "h8h10";
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        Puzzle puzzle = Puzzle.builder()
                .id(puzzleId)
                .rootBoardState("B...W...")
                .solutionDag(solutionDagBinary)
                .build();

        when(puzzleRepository.findById(puzzleId)).thenReturn(Optional.of(puzzle));
        when(solutionSerializer.deserialize(solutionDagBinary)).thenReturn(Map.of(300L, 44));

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzlePlayService.getAiResponse(puzzleId, currentBoardState)
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WRONG_MOVE);
    }

    @Test
    @DisplayName("퍼즐이 없으면 CANNOT_FIND_PUZZLE 예외가 발생한다")
    void getAiResponse_ShouldThrow_WhenPuzzleNotFound() {
        // given
        Long puzzleId = 999L;
        when(puzzleRepository.findById(puzzleId)).thenReturn(Optional.empty());

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzlePlayService.getAiResponse(puzzleId, "h8")
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CANNOT_FIND_PUZZLE);
    }

    @Test
    @DisplayName("보드 상태 문자열이 잘못되면 VALIDATION_ERROR 예외가 발생한다")
    void getAiResponse_ShouldThrowValidation_WhenBoardStateInvalid() {
        // given
        Long puzzleId = 10L;
        byte[] solutionDagBinary = new byte[] {1, 2, 3};
        Puzzle puzzle = Puzzle.builder()
                .id(puzzleId)
                .rootBoardState("B...W...")
                .solutionDag(solutionDagBinary)
                .build();

        when(puzzleRepository.findById(puzzleId)).thenReturn(Optional.of(puzzle));

        // when
        CustomException exception = assertThrows(
                CustomException.class,
                () -> puzzlePlayService.getAiResponse(puzzleId, "z99")
        );

        // then
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("저장 시 DAG가 MessagePack 직렬화되어 Puzzle 엔티티로 저장된다")
    void savePuzzle_ShouldSerializeAndPersist() {
        // given
        Map<Long, Integer> dag = Map.of(1L, 20, 2L, 40);
        byte[] serialized = new byte[] {9, 8, 7};
        when(solutionSerializer.serialize(dag)).thenReturn(serialized);
        when(puzzleRepository.save(any(Puzzle.class))).thenAnswer(invocation ->
                ((Puzzle) invocation.getArgument(0)).toBuilder().id(123L).build()
        );

        // when
        Long savedPuzzleId = puzzlePlayService.savePuzzle("ROOT_STATE", dag);

        // then
        assertThat(savedPuzzleId).isEqualTo(123L);
    }
}
