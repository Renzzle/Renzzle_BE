package com.renzzle.backend.domain.notice.util;

import com.renzzle.backend.global.common.domain.LangCode;

public class NoticeTextBuilderUtil {

    private NoticeTextBuilderUtil() {}

    public static String buildAttendanceMessage(LangCode langCode, int price) {
        return switch (LangCode.LangCodeName.valueOf(langCode.getName())) {
            case EN -> "You have received " + price + " pieces as an attendance reward.";
            case KO -> price + "피스를 출석 보상으로 획득하셨습니다.";
            case JA -> "ログインボーナスとして" + price + "ピースを獲得しました。";
            case ZH_HANS -> "您已获得 " + price + " 颗棋子作为签到奖励。";
            case ZH_HANT -> "您已獲得 " + price + " 顆棋子作為簽到獎勵。";
            // "шт." keeps the sentence right for any amount, which a declined noun would not
            case RU -> "Вы получили фишки (" + price + " шт.) за ежедневный вход.";
            case ET -> "Said igapäevase sisselogimise eest " + price + " nuppu.";
        };
    }

}
