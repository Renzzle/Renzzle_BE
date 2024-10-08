package com.renzzle.backend.domain.puzzle.domain;

import com.renzzle.backend.domain.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cascade;
import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "solved_lesson_puzzle")
public class SolvedLessonPuzzle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @Cascade(org.hibernate.annotations.CascadeType.REMOVE)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "lesson_id", nullable = false)
    @Cascade(org.hibernate.annotations.CascadeType.REMOVE)
    private LessonPuzzle puzzle;

    @Column(name = "solved_at", nullable = false)
    private Instant solvedAt;

}
