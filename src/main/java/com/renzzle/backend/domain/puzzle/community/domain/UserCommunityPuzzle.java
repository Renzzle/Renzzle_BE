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

    @Builder.Default
    @Column(name = "is_solved")
    private boolean isSolved = false;

    @Column(name = "solved_at")
    private Instant solvedAt;

    @Builder.Default
    @Column(name = "is_liked")
    private boolean isLiked = false;

    @Builder.Default
    @Column(name = "is_disliked")
    private boolean isDisliked = false;

    @Column(name = "liked_at")
    private Instant likedAt;

    public boolean toggleLike(Instant likedAt) {
        isLiked = !isLiked;
        if (isLiked) {
            isDisliked = false;
            this.likedAt = likedAt;
        } else {
            this.likedAt = null;
        }
        return isLiked;
    }

    public boolean toggleDislike() {
        isDisliked = !isDisliked;
        if (isDisliked) {
            isLiked = false;
        }
        return isDisliked;
    }

}
