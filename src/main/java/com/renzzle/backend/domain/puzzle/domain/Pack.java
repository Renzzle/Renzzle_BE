package com.renzzle.backend.domain.puzzle.domain;

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
    private Long packId;

    @Column(name = "puzzle_count", nullable = false)
    private int puzzle_count;

    @Column(name = "price", nullable = false)
    private int price;

    @ManyToOne
    @JoinColumn(name = "difficulty", nullable = false)
    private Difficulty difficulty;



}
