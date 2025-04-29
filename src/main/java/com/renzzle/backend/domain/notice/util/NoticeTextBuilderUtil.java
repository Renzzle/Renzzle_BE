package com.renzzle.backend.domain.notice.util;

import com.renzzle.backend.domain.user.domain.Title;
import com.renzzle.backend.global.common.domain.LangCode;
import java.util.Objects;

public class NoticeTextBuilderUtil {

    public static String buildAttendanceMessage(LangCode langCode, int price) {
        String code = langCode.getName();

//        String code = langCode.getCode();


        if (Objects.equals(code, LangCode.LangCodeName.KO.name())) {
            return price + "피스를 출석 보상으로 획득하셨습니다.";
        } else if (Objects.equals(code, LangCode.LangCodeName.EN.name())) {
            return "You have received " + price + "pieces as an attendance reward.";
        }

        return "You have received " + price + "pieces as an attendance reward.";
    }

    public static String buildGetTitleMessage(LangCode langCode, Title title) {
        String code = langCode.getName();

        String titleNameKo;
        String titleNameEn;

        switch (title.getName()) {
            case "MASTER" -> {
                titleNameKo = "장인";
                titleNameEn = "Master";
            }
            case "GRANDMASTER" -> {
                titleNameKo = "거장";
                titleNameEn = "Grandmaster";
            }
            case "PRO" -> {
                titleNameKo = "기사";
                titleNameEn = "Pro";
            }
            default -> {
                titleNameKo = "칭호 없음";
                titleNameEn = "No Title";
            }
        }

        if (Objects.equals(code, LangCode.LangCodeName.KO.name())) {
            return "축하합니다! '" + titleNameKo + "' 칭호를 획득하셨습니다.";
        } else if (Objects.equals(code, LangCode.LangCodeName.EN.name())) {
            return "Congratulations! You've earned the title: '" + titleNameEn + "'.";
        }

        return "Congratulations! You've earned the title: '" + titleNameEn + "'.";
    }

}
