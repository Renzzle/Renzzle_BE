package com.renzzle.backend.domain.user.domain;

import com.renzzle.backend.global.common.domain.Status;
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
@Table(name = "user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 15)
    private String nickname;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne
    @JoinColumn(name = "status", nullable = false)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "color", nullable = false)
    private Color color;

    @Setter
    @ManyToOne
    @JoinColumn(name = "level", nullable = false)
    private UserLevel level;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            this.status = Status.getDefaultStatus();
        }
        if (color == null) {
            this.color = Color.getRandomColor();
        }
        if (level == null) {
            this.level = UserLevel.getDefaultLevel();
        }
    }

}
