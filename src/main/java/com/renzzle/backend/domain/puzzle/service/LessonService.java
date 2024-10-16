package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.domain.puzzle.api.request.AddLessonPuzzleRequest;
import com.renzzle.backend.domain.puzzle.api.response.GetLessonPuzzleResponse;
import com.renzzle.backend.domain.puzzle.dao.LessonPuzzleRepository;
import com.renzzle.backend.domain.puzzle.dao.SolvedLessonPuzzleRepository;
import com.renzzle.backend.domain.puzzle.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.domain.LessonPuzzle;
import com.renzzle.backend.domain.puzzle.domain.SolvedLessonPuzzle;
import com.renzzle.backend.domain.puzzle.domain.WinColor;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import com.renzzle.backend.global.util.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonPuzzleRepository lessonPuzzleRepository;
    private final SolvedLessonPuzzleRepository solvedLessonPuzzleRepository;

    @Transactional
    public LessonPuzzle createLessonPuzzle(AddLessonPuzzleRequest request) {
        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        int index = lessonPuzzleRepository.findTopIndex(request.chapter()) + 1;
        if(request.puzzleIndex() != null && index > request.puzzleIndex()) {
            index = request.puzzleIndex();
            lessonPuzzleRepository.increaseIndexesFrom(request.chapter(), index);
        }

        LessonPuzzle puzzle = LessonPuzzle.builder()
                .chapter(request.chapter())
                .lessonIndex(index)
                .title(request.title())
                .boardStatus(request.boardStatus())
                .boardKey(boardKey)
                .depth(request.depth())
                .description(request.description())
                .difficulty(Difficulty.getDifficulty(request.difficulty()))
                .winColor(WinColor.getWinColor(request.winColor()))
                .build();

        return lessonPuzzleRepository.save(puzzle);
    }

    @Transactional
    public void deleteLessonPuzzle(Long lessonId) {
        Optional<LessonPuzzle> puzzle = lessonPuzzleRepository.findById(lessonId);
        if(puzzle.isEmpty())
            throw new CustomException(ErrorCode.CANNOT_FIND_LESSON_PUZZLE);

        lessonPuzzleRepository.deleteById(lessonId);
        lessonPuzzleRepository.decreaseIndexesFrom(puzzle.get().getChapter(), puzzle.get().getLessonIndex());
    }

    @Transactional
    public Long solveLessonPuzzle(UserEntity user, Long lessonId) {
        Optional<SolvedLessonPuzzle> existInfo
                = solvedLessonPuzzleRepository.findByUserIdAndLessonId(user.getId(), lessonId);

        // solve puzzle again
        if(existInfo.isPresent()) {
            return null;
        }

        LessonPuzzle lessonPuzzle = lessonPuzzleRepository.findById(lessonId).orElseThrow(
                () -> new CustomException(ErrorCode.CANNOT_FIND_LESSON_PUZZLE)
        );

        SolvedLessonPuzzle solvedLessonPuzzle = SolvedLessonPuzzle.builder()
                .user(user)
                .puzzle(lessonPuzzle)
                .build();
        solvedLessonPuzzleRepository.save(solvedLessonPuzzle);

        LessonPuzzle nextPuzzle = lessonPuzzleRepository
                .findByChapterAndIndex(lessonPuzzle.getChapter(), lessonPuzzle.getLessonIndex() + 1)
                .orElse(null);

        if(nextPuzzle == null) return null;
        else return nextPuzzle.getId();
    }

    @Transactional(readOnly = true)
    public List<GetLessonPuzzleResponse> getLessonPuzzleList(UserEntity user, int chapter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("lessonIndex").ascending());
        Page<LessonPuzzle> lessonPuzzles = lessonPuzzleRepository.findByChapter(chapter, pageable);

        if(lessonPuzzles.isEmpty()) {
            throw new CustomException(ErrorCode.NO_SUCH_LESSON_PAGE);
        }

        int solvedTopIndex = lessonPuzzleRepository.findTopSolvedPuzzleIndex(user.getId(), chapter);
        List<GetLessonPuzzleResponse> response = new ArrayList<>();
        lessonPuzzles.forEach(lessonPuzzle -> {
            boolean isLocked = solvedLessonPuzzleRepository.existsByUserAndPuzzle(user, lessonPuzzle);
            if(!isLocked && lessonPuzzle.getLessonIndex() == solvedTopIndex + 1)
                isLocked = true;

            response.add(GetLessonPuzzleResponse.builder()
                    .id(lessonPuzzle.getId())
                    .title(lessonPuzzle.getTitle())
                    .boardStatus(lessonPuzzle.getBoardStatus())
                    .depth(lessonPuzzle.getDepth())
                    .difficulty(lessonPuzzle.getDifficulty().getName())
                    .winColor(lessonPuzzle.getWinColor().getName())
                    .description(lessonPuzzle.getDescription())
                    .isLocked(isLocked)
                    .build());
        });

        return response;
    }

    @Transactional(readOnly = true)
    public double getLessonProgress(UserEntity user, int chapter) {
        int allLessonCnt = lessonPuzzleRepository.countAllLessonByChapter(chapter);
        if(allLessonCnt == 0) return 0.0;

        int solveCnt = solvedLessonPuzzleRepository.countSolvedLesson(user.getId(), chapter);

        return (double) solveCnt / allLessonCnt * 100;
    }

}
