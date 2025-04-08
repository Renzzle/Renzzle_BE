package com.renzzle.backend.domain.puzzle.training.domain;

import com.renzzle.backend.domain.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Clock;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "solved_training_puzzle",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "training_id"})
        }
)
public class SolvedTrainingPuzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "training_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private TrainingPuzzle puzzle;

    @CreationTimestamp
    @Column(name = "solved_at", updatable = false, nullable = false)
    private Instant solvedAt;

    public void updateSolvedAtToNow(Clock clock) {
        this.solvedAt = Instant.now(clock);
    }

}
