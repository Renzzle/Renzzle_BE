package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.domain.puzzle.api.request.AddLessonPuzzleRequest;
import com.renzzle.backend.domain.puzzle.dao.LessonPuzzleRepository;
import com.renzzle.backend.domain.puzzle.dao.SolvedLessonPuzzleRepository;
import com.renzzle.backend.domain.puzzle.domain.Difficulty;
import com.renzzle.backend.domain.puzzle.domain.LessonPuzzle;
import com.renzzle.backend.domain.puzzle.domain.WinColor;
import com.renzzle.backend.global.util.BoardUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonPuzzleRepository lessonPuzzleRepository;
    private final SolvedLessonPuzzleRepository solvedLessonPuzzleRepository;

    public LessonPuzzle createLessonPuzzle(AddLessonPuzzleRequest request) {
        String boardKey = BoardUtils.makeBoardKey(request.boardStatus());

        int index;
        if(request.puzzleIndex() == null) {
            index = lessonPuzzleRepository.findTopIndex(request.chapter()) + 1;
        } else {
            index = request.puzzleIndex();
            lessonPuzzleRepository.increaseIndexesFrom(index);
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

}
