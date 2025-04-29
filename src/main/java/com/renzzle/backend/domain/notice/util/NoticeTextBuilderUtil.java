package com.renzzle.backend.domain.notice.util;

import com.renzzle.backend.global.common.domain.LangCode;

import java.util.Objects;

public class NoticeTextBuilderUtil {

    public static String buildAttendanceMessage(LangCode langCode, int price) {
        String code = langCode.getCode();

        if (Objects.equals(code, LangCode.LangCodeName.KO.name())) {
            return price + "피스를 출석 보상으로 획득하셨습니다.";
        } else if (Objects.equals(code, LangCode.LangCodeName.EN.name())) {
            return "You have received " + price + "pieces as an attendance reward.";
        }

        return "You have received " + price + "pieces as an attendance reward.";
    }

}
