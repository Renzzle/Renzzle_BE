package com.renzzle.backend.domain.puzzle.training.domain;


import com.renzzle.backend.global.common.domain.LangCode;
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

    @ManyToOne
    @JoinColumn(name = "lang_code", nullable = false)
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    private LangCode langCode;

    @Column(name = "title", nullable = false, length = 225)
    private String title;

    @Column(name = "author", nullable = false, length = 127)
    private String author;

    @Column(name = "description", length = 1023)
    private String description;

}