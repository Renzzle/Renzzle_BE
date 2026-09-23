package com.renzzle.backend.domain.puzzle.rank.domain;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "latest_rank_puzzle")
public class LatestRankPuzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(name = "board_status", length = 1023, nullable = false)
    private String boardStatus;

    @Column(name = "answer", length = 1023, nullable = false)
    private String answer;

    // Whether it has been solved
    @Column(name = "is_solved", nullable = false)
    private Boolean isSolved;

    // Time assigned
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "win_color", nullable = false)
    private WinColor winColor;

    /*
        Snapshot of everything the rating math needs, taken when the puzzle was handed out.
        A rank game pre-deducts the loss at assignment time and reverts it on a solve, so the
        pre-deduction values are what every later calculation starts from. Keeping them on the
        row makes each assignment self-contained: the Redis session is only a liveness cache,
        and losing it can no longer corrupt a rating.
    */
    @Column(name = "puzzle_rating", nullable = false)
    private double puzzleRating;

    @Column(name = "rating_before_penalty", nullable = false)
    private double ratingBeforePenalty;

    @Column(name = "mmr_before_penalty", nullable = false)
    private double mmrBeforePenalty;

    @Column(name = "target_win_probability", nullable = false)
    private double targetWinProbability;

    public void solvedUpdate(boolean solved) {
        this.isSolved = solved;
    }
}
