package com.renzzle.backend.domain.puzzle.rank.domain;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

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

    // 사용자 아이디
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 보드 상태
    @Column(name = "board_status", length = 1023, nullable = false)
    private String boardStatus;

    // 정답
    @Column(name = "answer", length = 1023, nullable = false)
    private String answer;

    // 풀었는지 여부
    @Column(name = "is_solved", nullable = false)
    private Boolean isSolved;

    // 푼 시간
    @CreationTimestamp
    @Column(name = "solved_time", nullable = false, updatable = false)
    private Instant solvedTime;

    // 승리 색상 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "win_color", nullable = false)
    private WinColor winColor;

}
