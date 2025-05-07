package com.renzzle.backend.domain.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

@Entity
@Table(name = "title")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Title {

    @Id
    @Column(length = 31)
    private String name;

    private Title(String type) {
        this.name = type;
    }

    public enum TitleType {
        NONE, MASTER, GRANDMASTER, PRO
    }

    public static Title getDefaultTitle() {
        return new Title(TitleType.NONE.name());
    }

    public static Title getTitle(TitleType titleType) {
        return new Title(titleType.name());
    }

    public static Title getTitle(String typeName) {
        boolean isValid = Arrays.stream(TitleType.values())
                .anyMatch(tt -> tt.name().equalsIgnoreCase(typeName));
        if (!isValid) {
            throw new IllegalArgumentException("Invalid title type: " + typeName);
        }
        return new Title(typeName.toUpperCase());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Title) {
            return Objects.equals(this.name, ((Title) obj).name);
        } else if (obj instanceof TitleType) {
            return Objects.equals(this.name, ((TitleType) obj).name());
        } else {
            return false;
        }
    }

}
