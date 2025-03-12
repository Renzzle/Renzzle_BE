package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.domain.puzzle.api.request.AddTrainingPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.response.GetTrainingPuzzleResponse;
import com.renzzle.backend.domain.puzzle.dao.PackRepository;
import com.renzzle.backend.domain.puzzle.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.dao.SolvedTrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.domain.*;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.util.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingPuzzleRepository trainingPuzzleRepository;
    private final SolvedTrainingPuzzleRepository solvedTrainingPuzzleRepository;
    private final PackRepository packRepository;

    @Transactional
    public TrainingPuzzle createTrainingPuzzle(AddTrainingPuzzleRequest request) {
        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        int index = trainingPuzzleRepository.findTopIndex(request.packId()) + 1;
        if(request.puzzleIndex() != null && index > request.puzzleIndex()) {
            index = request.puzzleIndex();
            trainingPuzzleRepository.increaseIndexesFrom(request.packId(), index);
        }

        Pack pack = packRepository.findById(request.packId())
                .orElseThrow(() -> new CustomException(ErrorCode.CANNOT_FIND_PACK));

        // increase puzzle_count
        packRepository.increasePuzzleCount(request.packId());

        TrainingPuzzle puzzle = TrainingPuzzle.builder()
                .pack(pack)
                .trainingIndex(index)
                .boardStatus(request.boardStatus())
                .boardKey(boardKey)
                .depth(request.depth())
                .rating(request.rating())
                .winColor(WinColor.getWinColor(request.winColor()))
                .build();

        return trainingPuzzleRepository.save(puzzle);
    }

    @Transactional
    public void deleteTrainingPuzzle(Long puzzleId) {
        Optional<TrainingPuzzle> puzzle = trainingPuzzleRepository.findById(puzzleId);
        if(puzzle.isEmpty())
            throw new CustomException(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE);

        trainingPuzzleRepository.deleteById(puzzleId);
        trainingPuzzleRepository.decreaseIndexesFrom(puzzle.get().getTrainingIndex());
    }

    @Transactional
    public void solveLessonPuzzle(UserEntity user, Long puzzleId) {
        Optional<SolvedTrainingPuzzle> existInfo
                = solvedTrainingPuzzleRepository.findByUserIdAndLessonId(user.getId(), puzzleId);

        // solve puzzle again
        if(existInfo.isPresent()) {
            // ALREADY_SOLVED_PUZZLE 라는 에러를 만들어야 할까?
            return;
        }

        TrainingPuzzle trainingPuzzle = trainingPuzzleRepository.findById(puzzleId).orElseThrow(
                () -> new CustomException(ErrorCode.CANNOT_FIND_TRAINING_PUZZLE)
        );

        SolvedTrainingPuzzle solvedLessonPuzzle = SolvedTrainingPuzzle.builder()
                .user(user)
                .puzzle(trainingPuzzle)
                .build();
        solvedTrainingPuzzleRepository.save(solvedLessonPuzzle);
    }

    @Transactional(readOnly = true)
    public List<GetTrainingPuzzleResponse> getTrainingPuzzleList(UserEntity user, Long pack) {
        List<TrainingPuzzle> trainingPuzzles = trainingPuzzleRepository.findByPack_PackId(pack);

        if(trainingPuzzles.isEmpty()) {
            throw new CustomException(ErrorCode.NO_SUCH_TRAINING_PAGE);
        }

        List<GetTrainingPuzzleResponse> response = new ArrayList<>();
        trainingPuzzles.forEach(trainingPuzzle -> {
            boolean isSolved = solvedTrainingPuzzleRepository.existsByUserAndPuzzle(user, trainingPuzzle);

            response.add(GetTrainingPuzzleResponse.builder()
                    .id(trainingPuzzle.getId())
                    .boardStatus(trainingPuzzle.getBoardStatus())
                    .depth(trainingPuzzle.getDepth())
                    .winColor(trainingPuzzle.getWinColor().getName())
                    .isSolved(isSolved)
                    .build());
        });
        return response;
    }

    @Transactional(readOnly = true)
    public double getLessonProgress(UserEntity user, int chapter) {
        int allLessonCnt = trainingPuzzleRepository.countAllTrainingByChapter(chapter);
        if(allLessonCnt == 0) return 0.0;

        int solveCnt = solvedTrainingPuzzleRepository.countSolvedLesson(user.getId(), chapter);

        return (double) solveCnt / allLessonCnt * 100;
    }

}
