package com.renzzle.backend.domain.puzzle.community.web;

import com.renzzle.backend.domain.puzzle.community.api.response.GetSharedPuzzleResponse;
import com.renzzle.backend.global.common.domain.LangCode.LangCodeName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SharedPuzzlePageTextTest {

    private static final GetSharedPuzzleResponse PUZZLE = GetSharedPuzzleResponse.builder()
            .boardStatus("h8i9h9").authorName("omok").description(null)
            .depth(5).winColor("BLACK").isVerified(true)
            .solvedCount(132).views(841).likeCount(57)
            .build();

    @ParameterizedTest
    @NullAndEmptySource
    void languageOf_WithoutHeader_ThenEnglish(String acceptLanguage) {
        assertThat(SharedPuzzlePageText.languageOf(acceptLanguage)).isEqualTo(LangCodeName.EN);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "ko-KR,ko;q=0.9,en;q=0.8 | KO",
            "en-US,en;q=0.9,ko;q=0.8 | EN",
            "ja-JP,ja;q=0.9          | JA",
            "zh-CN,zh;q=0.9          | ZH_HANS",
            "zh                      | ZH_HANS",
            "zh-TW,zh;q=0.9          | ZH_HANT",
            "zh-Hant-HK              | ZH_HANT",
            "ru-RU,ru;q=0.9          | RU",
            "et-EE,et;q=0.9          | ET",
            "fr-FR,fr;q=0.9,ja;q=0.5 | JA",
            "fr-FR,fr;q=0.9          | EN",
            "de,ko;q=0               | EN",
            "@@@                     | EN"
    })
    void languageOf_PicksTheFirstLanguageThePageHas(String acceptLanguage, LangCodeName expected) {
        assertThat(SharedPuzzlePageText.languageOf(acceptLanguage)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "ko      | KO",
            "EN      | EN",
            "ja-JP   | JA",
            "zh-Hans | ZH_HANS",
            "zh-Hant | ZH_HANT",
            "zh-TW   | ZH_HANT",
            "ru      | RU",
            "et      | ET"
    })
    void languageOfParam_WhenTheLinkNamesALanguageThePageHas_ThenIt(String lang, LangCodeName expected) {
        assertThat(SharedPuzzlePageText.languageOfParam(lang)).isEqualTo(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"fr", "x<script>"})
    void languageOfParam_WhenTheLinkNamesNone_ThenNull(String lang) {
        assertThat(SharedPuzzlePageText.languageOfParam(lang)).isNull();
    }

    // A key missing from one language's file would fail the page there
    @ParameterizedTest
    @EnumSource(LangCodeName.class)
    void of_EveryLanguage_FillsEveryText(LangCodeName language) {
        SharedPuzzlePageText text = SharedPuzzlePageText.of(language, PUZZLE);

        Stream.of(text.lang(), text.title(), text.description(), text.goal(), text.meta(), text.counts(),
                        text.likes(), text.board(), text.retry(), text.bannerText(), text.bannerButton())
                .forEach(value -> assertThat(value).isNotBlank().doesNotContain("{", "}"));
        assertThat(text.authorPrefix() + text.authorSuffix()).isNotBlank().doesNotContain("{", "}");
        assertThat(text.title()).contains("omok");
        assertThat(text.meta()).contains("5");
        assertThat(text.counts()).contains("132", "841");
        assertThat(text.likes()).contains("57");

        SharedPuzzlePageText notFound = SharedPuzzlePageText.of(language, null);
        assertThat(notFound.title()).isNotBlank();
        assertThat(notFound.description()).isNotBlank();
    }

    @Test
    void of_TraditionalChinese_WritesTheLangTagAndTraditionalCharacters() {
        SharedPuzzlePageText text = SharedPuzzlePageText.of(LangCodeName.ZH_HANT, PUZZLE);

        assertThat(text.lang()).isEqualTo("zh-Hant");
        assertThat(text.goal()).isEqualTo("執黑連成五子");
        assertThat(text.authorPrefix() + "omok" + text.authorSuffix()).isEqualTo("omok 出題");
    }

}
