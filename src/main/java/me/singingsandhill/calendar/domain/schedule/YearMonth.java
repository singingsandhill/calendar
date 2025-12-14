package me.singingsandhill.calendar.domain.schedule;

import java.time.LocalDate;

public record YearMonth(int year, int month) {

    public YearMonth {
        if (year < 2024 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2024 and 2100");
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
    }

    public int getDaysInMonth() {
        return java.time.YearMonth.of(year, month).lengthOfMonth();
    }

    public int getFirstDayOfWeek() {
        return LocalDate.of(year, month, 1).getDayOfWeek().getValue() % 7;
    }

    public int calculateWeeks() {
        int firstDayOfWeek = getFirstDayOfWeek();
        int totalDays = firstDayOfWeek + getDaysInMonth();
        return (int) Math.ceil(totalDays / 7.0);
    }

    public static YearMonth of(int year, int month) {
        return new YearMonth(year, month);
    }
}
