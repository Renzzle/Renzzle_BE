package com.renzzle.backend.domain.user.domain;

import com.renzzle.backend.global.common.domain.Status;
import jakarta.persistence.*;
import lombok.*;
import org.apache.catalina.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import static com.renzzle.backend.global.common.constant.TimeConstant.CONST_FUTURE_INSTANT;
import static com.renzzle.backend.global.common.domain.Status.STATUS_IS_NOT_DELETED;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"email", "status", "deleted_at"}),
                @UniqueConstraint(columnNames = {"nickname", "status", "deleted_at"}),
        }
)
@SQLRestriction(value = STATUS_IS_NOT_DELETED)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "nickname", nullable = false, length = 31)
    private String nickname;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    @ManyToOne
    @JoinColumn(name = "status", nullable = false)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "color", nullable = false)
    private Color color;

    @ManyToOne
    @JoinColumn(name = "level", nullable = false)
    private UserLevel level;

    public void setUserLevel(UserLevel UserLevel){
        this.level = UserLevel;
    }

    @PrePersist
    public void onPrePersist() {
        if(status == null) {
            this.status = Status.getDefaultStatus();
        }
        if(deletedAt == null) {
            this.deletedAt = CONST_FUTURE_INSTANT;
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
        this.status = Status.getStatus(Status.StatusName.DELETED);
        this.deletedAt = Instant.now();
    }

}
