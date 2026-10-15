package me.singingsandhill.calendar.datedate.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * /insights/trends 모임 데이터 리포트 (ADR datedate/domain/0013). 블록은 표본 하한 미달이면 null — 템플릿이 숨긴다.
 */
public record MeetupReportDto(LocalDate asOf, WeekdayBlock weekdays, MonthlyBlock monthly, LeadTimeBlock leadTime) {

    /** key 는 메시지 키 꼬리(요일은 DayOfWeek.name(), 리드타임은 D0_6 등), percent 는 정수 %(블록 합계 100). */
    public record Share(String key, int percent) {
    }

    public record WeekdayBlock(int sampleSize, List<Share> shares) {
    }

    public record MonthCount(int year, int month, int count, int barPercent, boolean inProgress) {
    }

    public record MonthlyBlock(int total, List<MonthCount> months) {
    }

    public record LeadTimeBlock(int sampleSize, int medianDays, List<Share> buckets) {
    }

    public boolean hasAnyBlock() {
        return weekdays != null || monthly != null || leadTime != null;
    }
}
