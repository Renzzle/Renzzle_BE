package com.renzzle.backend.domain.puzzle.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "training_puzzle",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"pack_id, training_index"})
        }
)
public class TrainingPuzzle {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne
        @JoinColumn(name = "pack_id", nullable = false)
        @OnDelete(action = OnDeleteAction.CASCADE)
        private Pack pack;

        @Column(name = "training_index", nullable = false)
        private int trainingIndex;

        @Column(name = "board_status", nullable = false, length = 1023)
        private String boardStatus;

        @Column(name = "board_key", unique = true, nullable = false)
        private String boardKey;

        @Column(name = "answer", nullable = false, length = 1023)
        private String answer;

        @Column(name = "depth", nullable = false)
        private int depth;

        @Column(name = "rating", nullable = false)
        private double rating;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false, nullable = false)
        private Instant createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private Instant updatedAt;

        @ManyToOne
        @JoinColumn(name = "win_color", nullable = false)
        private WinColor winColor;

}
