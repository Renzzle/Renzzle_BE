package com.renzzle.backend.domain.puzzle.domain;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class PackTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pack_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Pack pack;

    @Column(name = "language_code", nullable = false, length = 15)
    private String languageCode;

    @Column(name = "title", nullable = false, length = 225)
    private String title;

    @Column(name = "author", nullable = false, length = 127)
    private String author;

    @Column(name = "description", length = 1023)
    private String description;

}

