package com.renzzle.backend.domain.puzzle.community.domain;

import com.renzzle.backend.domain.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Clock;
import java.time.Instant;

import static com.renzzle.backend.global.common.constant.TimeConstant.CONST_FUTURE_INSTANT;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "user_community_puzzle",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "community_id"})
        }
)
public class UserCommunityPuzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "community_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommunityPuzzle puzzle;

    @Builder.Default
    @Column(name = "is_solved")
    private boolean isSolved = false;

    @Column(name = "solved_at", nullable = false)
    private Instant solvedAt;

    @Builder.Default
    @Column(name = "like")
    private boolean like = false;

    @Builder.Default
    @Column(name = "dislike")
    private boolean dislike = false;

    @PrePersist
    public void prePersist() {
        if(solvedAt == null) {
            this.solvedAt = CONST_FUTURE_INSTANT;
        }
    }

    public void solvePuzzle(Clock clock) {
        isSolved = true;
        solvedAt = clock.instant();
    }

}
