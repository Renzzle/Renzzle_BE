package com.renzzle.backend.domain.puzzle.rank.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankSessionData {
    private Long userId;
    private String boardState;
    private double lastProblemRating;
    private String winnerColor;
    private double mmrBeforePenalty;
    private double ratingBeforePenalty;
    private double targetWinProbability;
}
