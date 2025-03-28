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
    private double lastProblemRating;  // 레이팅 변경 전 문제 레이팅
    private String winnerColor;
    private double initialMmr;
    private double ratingBeforePenalty;
    private double mmrBeforePenalty;
}
