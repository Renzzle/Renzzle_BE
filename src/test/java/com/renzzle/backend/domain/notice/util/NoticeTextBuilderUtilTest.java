package com.renzzle.backend.domain.notice.util;

import com.renzzle.backend.global.common.domain.LangCode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeTextBuilderUtilTest {

    @Test
    void buildAttendanceMessage_WhenAnyLanguage_ThenStatesTheAmountInItsOwnWords() {
        Set<String> messages = new HashSet<>();
        for (LangCode.LangCodeName lang : LangCode.LangCodeName.values()) {
            String message = NoticeTextBuilderUtil.buildAttendanceMessage(LangCode.getLangCode(lang), 200);

            assertThat(message).contains("200");
            messages.add(message);
        }

        // No language falls back to another's text
        assertThat(messages).hasSize(LangCode.LangCodeName.values().length);
    }

    @Test
    void buildAttendanceMessage_WhenEnglish_ThenSpacesTheAmountFromTheWord() {
        assertThat(NoticeTextBuilderUtil.buildAttendanceMessage(LangCode.getLangCode(LangCode.LangCodeName.EN), 200))
                .isEqualTo("You have received 200 pieces as an attendance reward.");
    }
}
