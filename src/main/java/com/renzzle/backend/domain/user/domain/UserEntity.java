package com.renzzle.backend.domain.user.domain;

import com.renzzle.backend.global.common.domain.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

import static com.renzzle.backend.global.common.domain.Status.DELETED_NICKNAME_SUFFIX;
import static com.renzzle.backend.global.common.domain.Status.STATUS_RESTRICTION;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"email", "status"}),
        }
)
@SQLRestriction(value = STATUS_RESTRICTION)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 31)
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

    @ManyToOne
    @JoinColumn(name = "level", nullable = false)
    private UserLevel level;

    @PrePersist
    public void onPrePersist() {
        if(status == null) {
            this.status = Status.getDefaultStatus();
        }
        if(color == null) {
            this.color = Color.getRandomColor();
        }
        if(level == null) {
            this.level = UserLevel.getDefaultLevel();
        }
    }

    @PreRemove
    public void onPreRemove() {
        this.status.setStatus(Status.StatusName.DELETED);
        this.nickname += DELETED_NICKNAME_SUFFIX;
    }

}
