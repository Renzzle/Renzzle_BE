package com.renzzle.backend.domain.puzzle.community.domain;

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

    @Column(name = "last_tried_at", nullable = false)
    private Instant lastTriedAt;

    @Column(name = "solved_count", nullable = false)
    @Builder.Default
    private int solvedCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private int failedCount = 0;

    @Column(name = "is_liked", nullable = false)
    @Builder.Default
    private boolean like = false;

    public int addSolve() {
        this.solvedCount++;
        lastTriedAt = Instant.now();
        return this.solvedCount;
    }

    public int addFail() {
        this.failedCount++;
        lastTriedAt = Instant.now();
        return this.failedCount;
    }

    public boolean toggleLike() {
        this.like = !this.like;
        return this.like;
    }

}
