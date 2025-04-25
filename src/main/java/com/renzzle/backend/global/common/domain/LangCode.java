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
    private String code;

    public enum LangCodeName {
        EN, KO
    }

    private LangCode(String code) {
        this.code = code;
    }

    public static LangCode getLangCode(LangCodeName langCodeName) {
        return new LangCode(langCodeName.name());
    }

    public static LangCode getLangCode(String langCodeName) {
        LangCode langCode = new LangCode();

        LangCode.LangCodeName[] langCodeNames = LangCode.LangCodeName.values();
        boolean isValid = Arrays.stream(langCodeNames)
                .anyMatch(lc -> lc.name().equals(langCodeName));
        if (!isValid)
            throw new IllegalArgumentException("Invalid lang code name: " + langCodeName);

        langCode.code = langCodeName;

        return langCode;
    }

}
