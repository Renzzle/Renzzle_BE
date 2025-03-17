package com.renzzle.backend.domain.user.domain;

import com.renzzle.backend.global.common.constant.DoubleConstant;
import com.renzzle.backend.global.common.constant.ItemPrice;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
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
                @UniqueConstraint(columnNames = {"device_id", "status", "deleted_at"})
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

    @Column(name = "rating")
    private double rating = DoubleConstant.DEFAULT_RATING;

    @Column(name = "mmr")
    private double mmr = DoubleConstant.DEFAULT_RATING;

    @Column(name = "currency")
    private int currency = 0;

    @Column(name = "device_id", nullable = false, length = 1024)
    private String deviceId;

    @Column(name = "lastAccessedAt", nullable = false)
    private Instant lastAccessedAt;

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

    @PrePersist
    public void onPrePersist() {
        if(status == null) {
            this.status = Status.getDefaultStatus();
        }
        if(lastAccessedAt == null) {
            this.lastAccessedAt = Instant.now();
        }
        if(deletedAt == null) {
            this.deletedAt = CONST_FUTURE_INSTANT;
        }
    }

    public void softDelete() {
        this.status = Status.getStatus(Status.StatusName.DELETED);
        this.deletedAt = Instant.now();
    }

    public void changeNickname(String nickname) {
        if(this.currency < ItemPrice.CHANGE_NICKNAME.getPrice())
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        this.nickname = nickname;
        this.currency -= ItemPrice.CHANGE_NICKNAME.getPrice();
    }

}
