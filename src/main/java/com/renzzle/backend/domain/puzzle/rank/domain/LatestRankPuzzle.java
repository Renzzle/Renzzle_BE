package com.renzzle.backend.domain.puzzle.rank.domain;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
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

    // 풀었는지 여부
    @Column(name = "is_solved", nullable = false)
    private Boolean isSolved;

    // 출제 시간
    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "win_color", nullable = false)
    private WinColor winColor;

    public void solvedUpdate(boolean solved) {
        this.isSolved = solved;
    }
}
