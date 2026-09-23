package com.renzzle.backend.domain.puzzle.rank.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
    Liveness marker for an in-progress rank game, held in Redis under a TTL.
    It deliberately carries no rating state: every value the rating math needs lives on the
    matching LatestRankPuzzle row, so a lost or expired session can never skew a rating.
*/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankSessionData {
    private Long userId;
    private String boardState;
    private String winnerColor;
    private boolean isStarted = false;
}
