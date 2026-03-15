package com.renzzle.backend.domain.puzzle.cache.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "cache_puzzle",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_type_source", columnNames = {"puzzle_type", "source_id"})
        },
        indexes = {
                @Index(name = "idx_root_board_state", columnList = "root_board_state")
        }
)
public class Puzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "puzzle_type", nullable = false, length = 20)
    private PuzzleType puzzleType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "root_board_state", nullable = false, length = 500)
    private String rootBoardState;

    @Lob
    @Column(name = "solution_dag", columnDefinition = "MEDIUMBLOB")
    private byte[] solutionDag;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
