package com.renzzle.backend.domain.puzzle.training.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class Pack {

    // Granted on sign-up
    public static final Long STARTER_PACK_ID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "puzzle_count", nullable = false)
    private int puzzleCount;


    @Column(name = "price", nullable = false)
    private int price;

    @ManyToOne
    @JoinColumn(name = "difficulty", nullable = false)
    private Difficulty difficulty;

    // Default true so packs that existed before the column stay public when ddl-auto adds it
    @Builder.Default
    @ColumnDefault("true")
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    // User id of the admin who created the pack; null for packs from before the column, which only operators manage
    @Column(name = "created_by")
    private Long createdBy;
}
