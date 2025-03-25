package com.renzzle.backend.domain.puzzle.training.domain;

import com.renzzle.backend.domain.puzzle.shared.domain.Difficulty;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class Pack {

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
}
