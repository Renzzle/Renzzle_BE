package com.renzzle.backend.domain.puzzle.service;

import com.renzzle.backend.domain.puzzle.dao.LessonPuzzleRepository;
import com.renzzle.backend.domain.puzzle.dao.SolvedLessonPuzzleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonPuzzleRepository lessonPuzzleRepository;
    private final SolvedLessonPuzzleRepository solvedLessonPuzzleRepository;

}
