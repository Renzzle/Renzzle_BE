package com.renzzle.backend.domain.puzzle.rank.util;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class TrainingPuzzleSeeder {
    @Autowired
    private TrainingPuzzleRepository trainingPuzzleRepository;
    @Autowired
    private PackSeeder packSeeder;

    public void seedPuzzle(
            int trainingIndex,
            String boardStatus,
            String answer,
            int depth,
            double rating,
            String winColor
    ) {
        Pack pack = packSeeder.seedPack("MIDDLE", 1, 0); // 원하는 난이도와 퍼즐 수, 가격 설정 가능

        TrainingPuzzle puzzle = TrainingPuzzle.builder()
                .trainingIndex(trainingIndex)
                .boardStatus(boardStatus)
                .boardKey(BoardUtils.makeBoardKey(boardStatus))
                .answer(answer)
                .depth(depth)
                .rating(rating)
                .winColor(WinColor.getWinColor(winColor))
                .pack(pack)
                .build();

        trainingPuzzleRepository.save(puzzle);
    }
}
