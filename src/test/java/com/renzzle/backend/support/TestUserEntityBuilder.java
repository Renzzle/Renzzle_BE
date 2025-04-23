package com.renzzle.backend.support;

import com.renzzle.backend.domain.user.dao.UserRepository;
import com.renzzle.backend.domain.user.domain.UserEntity;
import com.renzzle.backend.global.common.domain.Status;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

import static com.renzzle.backend.global.common.constant.TimeConstant.CONST_FUTURE_INSTANT;

public class TestUserEntityBuilder {

    private static Long idx = 0L;

    private Long id = null;
    private String email = UUID.randomUUID() + "@test.com";
    private String password = "password";
    private String nickname = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    private double rating = 1000.0;
    private double mmr = 1000.0;
    private int currency = 0;
    private String deviceId = UUID.randomUUID().toString();
    private Instant lastAccessedAt = null;
    private Instant createdAt = null;
    private Instant updatedAt = null;
    private Instant deletedAt = null;
    private Status status = null;

    public static TestUserEntityBuilder builder() {
        return new TestUserEntityBuilder();
    }

    public TestUserEntityBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public TestUserEntityBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public TestUserEntityBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public TestUserEntityBuilder withNickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public TestUserEntityBuilder withRating(double rating) {
        this.rating = rating;
        return this;
    }

    public TestUserEntityBuilder withMmr(double mmr) {
        this.mmr = mmr;
        return this;
    }

    public TestUserEntityBuilder withCurrency(int currency) {
        this.currency = currency;
        return this;
    }

    public TestUserEntityBuilder withDeviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public TestUserEntityBuilder withLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
        return this;
    }

    public TestUserEntityBuilder withCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public TestUserEntityBuilder withUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public TestUserEntityBuilder withDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
        return this;
    }

    public TestUserEntityBuilder withStatus(Status status) {
        this.status = status;
        return this;
    }

    public UserEntity build() {
        return UserEntity.builder()
                .id(id)
                .email(email)
                .password(password)
                .nickname(nickname)
                .rating(rating)
                .mmr(mmr)
                .currency(currency)
                .deviceId(deviceId)
                .lastAccessedAt(lastAccessedAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .status(status)
                .build();
    }

    public UserEntity save(UserRepository repository) {
        if (repository == null || Mockito.mockingDetails(repository).isMock()) {
            id = (id == null) ? ++idx : id;
            status = (status == null) ? Status.getDefaultStatus() : status;
            lastAccessedAt = (lastAccessedAt == null) ? Instant.now() : lastAccessedAt;
            createdAt = (createdAt == null) ? Instant.now() : createdAt;
            updatedAt = (updatedAt == null) ? Instant.now() : updatedAt;
            deletedAt = (deletedAt == null) ? CONST_FUTURE_INSTANT : deletedAt;
            return build();
        } else {
            UserEntity user = repository.save(build());
            idx = user.getId();
            return user;
        }
    }

}
