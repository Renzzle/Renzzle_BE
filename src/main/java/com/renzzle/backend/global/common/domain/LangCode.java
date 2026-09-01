package com.renzzle.backend.global.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Entity
@Table(name = "lang_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LangCode {

    @Id
    @Column(length = 15)
    private String name;

    public enum LangCodeName {
        EN, KO
    }

    private LangCode(String code) {
        this.name = code;
    }

    public static LangCode getLangCode(LangCodeName langCodeName) {
        return new LangCode(langCodeName.name());
    }

    public static LangCode getLangCode(String langCodeName) {
        return Arrays.stream(LangCodeName.values())
                .filter(lc -> lc.name().equalsIgnoreCase(langCodeName))
                .findFirst()
                .map(LangCode::getLangCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid lang code name: " + langCodeName));
    }

}
