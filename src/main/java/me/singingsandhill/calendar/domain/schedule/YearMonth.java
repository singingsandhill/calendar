package me.singingsandhill.calendar.domain.schedule;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record YearMonth(int year, int month) {

    public static final int FIXED_WEEKS = 7;
    public static final int FIXED_TOTAL_DAYS = 49;

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

    /**
     * 7주 달력 그리드의 시작 날짜를 반환합니다.
     * 월 1일이 속한 주의 일요일입니다.
     */
    public LocalDate getGridStartDate() {
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        int firstDayOfWeek = getFirstDayOfWeek();
        return firstOfMonth.minusDays(firstDayOfWeek);
    }

    /**
     * 인덱스(1~49)를 실제 LocalDate로 변환합니다.
     */
    public LocalDate indexToDate(int index) {
        if (index < 1 || index > FIXED_TOTAL_DAYS) {
            throw new IllegalArgumentException("Index must be between 1 and " + FIXED_TOTAL_DAYS);
        }
        return getGridStartDate().plusDays(index - 1);
    }

    /**
     * LocalDate를 인덱스(1~49)로 변환합니다.
     */
    public int dateToIndex(LocalDate date) {
        LocalDate gridStart = getGridStartDate();
        long daysBetween = ChronoUnit.DAYS.between(gridStart, date);
        int index = (int) daysBetween + 1;

        if (index < 1 || index > FIXED_TOTAL_DAYS) {
            throw new IllegalArgumentException("Date is outside the 7-week range");
        }
        return index;
    }

    /**
     * 해당 인덱스가 선택된 월에 속하는지 확인합니다.
     */
    public boolean isInSelectedMonth(int index) {
        LocalDate date = indexToDate(index);
        return date.getYear() == year && date.getMonthValue() == month;
    }

    public static YearMonth of(int year, int month) {
        return new YearMonth(year, month);
    }
}
