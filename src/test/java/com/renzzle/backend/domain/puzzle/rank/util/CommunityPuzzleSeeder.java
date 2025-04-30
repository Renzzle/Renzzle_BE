package com.renzzle.backend.domain.puzzle.rank.util;

import com.renzzle.backend.domain.puzzle.community.dao.CommunityPuzzleRepository;
import com.renzzle.backend.domain.puzzle.community.domain.CommunityPuzzle;
import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.puzzle.shared.util.BoardUtils;
import com.renzzle.backend.domain.user.domain.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
//@Profile("test")
public class CommunityPuzzleSeeder {

    @Autowired
    private CommunityPuzzleRepository communityPuzzleRepository;

    public void seedPuzzle(
            String boardStatus,
            String answer,
            int depth,
            double rating,
            String winColor,
            UserEntity author
    ) {
        CommunityPuzzle puzzle = CommunityPuzzle.builder()
                .boardStatus(boardStatus)
                .boardKey(BoardUtils.makeBoardKey(boardStatus))
                .answer(answer)
                .depth(depth)
                .rating(rating)
                .isVerified(true)
                .winColor(WinColor.getWinColor(winColor))
                .user(author)
                .build();

        communityPuzzleRepository.save(puzzle);
    }
}
