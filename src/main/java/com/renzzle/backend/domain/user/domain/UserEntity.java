package com.renzzle.backend.domain.user.domain;

import com.renzzle.backend.global.common.constant.DoubleConstant;
import com.renzzle.backend.global.common.constant.ItemPrice;
import com.renzzle.backend.global.common.domain.Status;
import com.renzzle.backend.global.exception.CustomException;
import com.renzzle.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

import static com.renzzle.backend.global.common.constant.StringConstant.DELETED_USER;
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
@EqualsAndHashCode(of = "id")
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

    @Builder.Default
    @Column(name = "rating")
    private double rating = DoubleConstant.DEFAULT_RATING;

    @Builder.Default
    @Column(name = "mmr")
    private double mmr = DoubleConstant.DEFAULT_RATING;

    @Builder.Default
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

    @ManyToOne
    @JoinColumn(name = "title", nullable = false)
    private Title title;

    @PrePersist
    public void onPrePersist() {
        if (status == null) {
            this.status = Status.getDefaultStatus();
        }
        if (title == null) {
            this.title = Title.getDefaultTitle();
        }
        if (lastAccessedAt == null) {
            this.lastAccessedAt = Instant.now();
        }
        if (deletedAt == null) {
            this.deletedAt = CONST_FUTURE_INSTANT;
        }
    }

    public String getNickname() {
        return status.equals(Status.getStatus(Status.StatusName.DELETED))
                ? DELETED_USER : nickname;
    }

    public void purchase(int price){
        if (this.currency < price)
            throw new CustomException(ErrorCode.INSUFFICIENT_CURRENCY);

        this.currency -= price;
    }

    public void updateRatingTo(double newRating) {
        this.rating = newRating;
    }

    public void updateMmrTo(double newMmr) {
        this.mmr = newMmr;
    }

    public void softDelete() {
        this.status = Status.getStatus(Status.StatusName.DELETED);
        this.deletedAt = Instant.now();
    }

    public void changeNickname(String nickname) {
        purchase(ItemPrice.CHANGE_NICKNAME.getPrice());
        this.nickname = nickname;
    }
}
