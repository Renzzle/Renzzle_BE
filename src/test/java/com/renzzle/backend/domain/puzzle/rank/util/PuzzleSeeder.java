package com.renzzle.backend.domain.puzzle.rank.util;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.domain.puzzle.training.dao.TrainingPuzzleRepository;
import com.renzzle.backend.domain.puzzle.training.domain.Pack;
import com.renzzle.backend.domain.puzzle.training.domain.TrainingPuzzle;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class PuzzleSeeder {

    @Autowired
    private TrainingPuzzleRepository trainingPuzzleRepository;
    @Autowired
    private CommunityPuzzleSeeder communityPuzzleSeeder;
    @Autowired
    private PackSeeder packSeeder;

    public void seedTrainingPuzzle(
            int trainingIndex,
            String boardStatus,
            String answer,
            int depth,
            double rating,
            String winColor
    ) {
        Pack pack = packSeeder.seedPack("MIDDLE", 1, 0);

        trainingPuzzleRepository.save(
                TrainingPuzzle.builder()
                        .pack(pack)
                        .trainingIndex(trainingIndex)
                        .boardStatus(boardStatus)
                        .boardKey(BoardUtils.makeBoardKey(boardStatus))
                        .answer(answer)
                        .depth(depth)
                        .rating(rating)
                        .winColor(WinColor.getWinColor(winColor))
                        .build()
        );
    }

    public void seedCommunityPuzzle(
            String boardStatus,
            String answer,
            int depth,
            double rating,
            String winColor,
            UserEntity author
    ) {
        communityPuzzleSeeder.seedPuzzle(boardStatus, answer, depth, rating, winColor, author);
    }
}
