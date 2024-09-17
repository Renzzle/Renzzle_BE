package com.renzzle.backend.domain.user.domain;

import com.renzzle.backend.global.common.domain.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(name = "user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 15)
    private String nickname;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name = "status", nullable = false)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "color", nullable = false)
    private Color color;

    @ManyToOne
    @JoinColumn(name = "level", nullable = false)
    private Level level;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            this.status = Status.getDefaultStatus();
        }
        if (color == null) {
            this.color = Color.getRandomColor();
        }
        if (level == null) {
            this.level = Level.getDefaultLevel();
        }
    }

}
