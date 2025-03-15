package com.renzzle.backend.domain.puzzle.domain;


import com.renzzle.backend.domain.user.domain.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserPack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userPackId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "pack_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Pack pack;

    @Column(name = "solved_count", nullable = false)
    private int solved_count;
}
