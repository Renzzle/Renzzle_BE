package com.renzzle.backend.global.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "status")
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

    public void setStatus(StatusName statusName) {
        this.name = statusName.name();
    }

}
