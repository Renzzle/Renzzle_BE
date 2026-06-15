package com.renzzle.backend.global.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Entity
@Table(name = "status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Status {

    public static final String STATUS_IS_NOT_DELETED = "status != 'DELETED'";

    @Id
    @Column(length = 31)
    private String name;

    private Status(String name) {
        this.name = name;
    }

    public enum StatusName {
        CREATED, DELETED
    }

    public static Status getDefaultStatus() {
        return new Status(StatusName.CREATED.name());
    }

    public static Status getStatus(StatusName statusName) {
        Status status = new Status();
        status.name = statusName.name();
        return status;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Status status) {
            return Objects.equals(this.name, status.name);
        } else if (obj instanceof StatusName statusName) {
            return Objects.equals(this.name, statusName.name());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

}
