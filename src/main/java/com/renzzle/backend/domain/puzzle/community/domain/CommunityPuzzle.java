package com.renzzle.backend.domain.puzzle.community.domain;

import com.renzzle.backend.domain.puzzle.shared.domain.WinColor;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.*;

import java.time.Instant;

import static com.renzzle.backend.global.common.constant.TimeConstant.CONST_FUTURE_INSTANT;
import static com.renzzle.backend.global.common.domain.Status.STATUS_IS_NOT_DELETED;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "community_puzzle",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"board_key", "status", "deleted_at"})
        },
        indexes = {
                @Index(columnList = "created_at"),
                @Index(columnList = "like_count")
        }
)
@SQLRestriction(value = STATUS_IS_NOT_DELETED)
public class CommunityPuzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_status", nullable = false, length = 1023)
    private String boardStatus;

    @Column(name = "board_key", nullable = false)
    private String boardKey;

    @Column(name = "answer", nullable = false, length = 1023)
    private String answer;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @Column(name = "isVerified", nullable = false)
    private Boolean isVerified;

    @Column(name = "rating", nullable = false)
    private Double rating;

    @Builder.Default
    @Column(name = "solved_count")
    private int solvedCount = 0;

    @Builder.Default
    @Column(name = "like_count")
    private int likeCount = 0;

    @Builder.Default
    @Column(name = "dislike_count")
    private int dislikeCount = 0;

    @Builder.Default
    @Column(name = "view")
    private int view = 0;

    @Column(name = "description", length = 127)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    @ManyToOne
    @JoinColumn(name = "status", nullable = false)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "win_color", nullable = false)
    private WinColor winColor;

    @PrePersist
    public void prePersist() {
        if(status == null) {
            this.status = Status.getDefaultStatus();
        }
        if(deletedAt == null) {
            this.deletedAt = CONST_FUTURE_INSTANT;
        }
    }
}
