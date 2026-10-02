package com.renzzle.backend.domain.puzzle.rank.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Liveness marker only; rating state lives on LatestRankPuzzle
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankSessionData {
    private Long userId;
    private String boardState;
    private String winnerColor;
    private boolean isStarted = false;
}
