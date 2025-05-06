package com.renzzle.backend.domain.puzzle.content.util;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;

public record YearWeek(int year, int week) {
    public static YearWeek from(LocalDate date) {
        WeekFields wf = WeekFields.of(Locale.KOREA);
        return new YearWeek(date.getYear(), date.get(wf.weekOfWeekBasedYear()));
    }

    public static YearWeek of(int year, int week) {
        return new YearWeek(year, week);
    }

}
