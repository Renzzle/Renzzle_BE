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

    public YearWeek minusWeeks(int n) {
        LocalDate ref = LocalDate.ofYearDay(this.year, 1).with(WeekFields.ISO.weekOfYear(), this.week);
        LocalDate minus = ref.minusWeeks(n);
        return from(minus);
    }

    public boolean isBefore(YearWeek other) {
        return this.year < other.year || (this.year == other.year && this.week < other.week);
    }

    public String toDatabaseKey() {
        return String.format("%d%02d", year, week);
    }
}
