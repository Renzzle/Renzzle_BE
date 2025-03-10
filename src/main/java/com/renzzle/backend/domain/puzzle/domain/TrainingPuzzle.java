package com.renzzle.backend.domain.puzzle.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "lesson_puzzle",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"chapter, lesson_index"})
        }
)
public class TrainingPuzzle {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "chapter", nullable = false)
        private int chapter;

        @Column(name = "lesson_index", nullable = false)
        private int lessonIndex;

        @Column(name = "title", nullable = false, length = 31)
        private String title;

        @Column(name = "board_status", nullable = false, length = 1023)
        private String boardStatus;

        @Column(name = "board_key", unique = true, nullable = false)
        private String boardKey;

        @Column(name = "depth", nullable = false)
        private int depth;

        @Column(name = "description")
        private String description;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false, nullable = false)
        private Instant createdAt;

        @UpdateTimestamp
        @Column(name = "updated_at", nullable = false)
        private Instant updatedAt;

        @ManyToOne
        @JoinColumn(name = "difficulty", nullable = false)
        private Difficulty difficulty;

        @ManyToOne
        @JoinColumn(name = "win_color", nullable = false)
        private WinColor winColor;

}
