package me.singingsandhill.calendar.datedate.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.LeadTimeBlock;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.MonthCount;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.MonthlyBlock;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.Share;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.WeekdayBlock;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantAvailability;

/**
 * 모임 데이터 리포트 계산 — 스프링 의존 없는 순수 계산 (ADR datedate/domain/0013).
 * 표본 하한 미달 블록은 null. 비율은 최대 잉여 반올림으로 합계 100.
 */
public final class MeetupReportCalculator {

    static final int WEEKDAY_MIN_DATES = 300;
    static final int MONTHLY_MIN_SCHEDULES = 30;
    static final int LEAD_TIME_MIN_SCHEDULES = 30;
    static final int MONTHS_SHOWN = 12;

    private static final List<DayOfWeek> WEEK_ORDER = List.of(
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
    private static final List<String> LEAD_BUCKETS = List.of("D0_6", "D7_13", "D14_27", "D28_PLUS");

    private MeetupReportCalculator() {
    }

    public static MeetupReportDto calculate(List<ParticipantAvailability> availabilities,
                                            List<LocalDateTime> scheduleCreatedAts,
                                            LocalDate today) {
        return new MeetupReportDto(today,
                weekdays(availabilities),
                monthly(scheduleCreatedAts, today),
                leadTime(availabilities));
    }

    private static WeekdayBlock weekdays(List<ParticipantAvailability> availabilities) {
        Map<DayOfWeek, Integer> counts = new EnumMap<>(DayOfWeek.class);
        int total = 0;
        for (ParticipantAvailability a : availabilities) {
            for (LocalDate date : a.availableDates()) {
                counts.merge(date.getDayOfWeek(), 1, Integer::sum);
                total++;
            }
        }
        if (total < WEEKDAY_MIN_DATES) {
            return null;
        }
        int[] percents = percentages(WEEK_ORDER.stream().mapToInt(d -> counts.getOrDefault(d, 0)).toArray());
        List<Share> shares = IntStream.range(0, WEEK_ORDER.size())
                .mapToObj(i -> new Share(WEEK_ORDER.get(i).name(), percents[i]))
                .toList();
        return new WeekdayBlock(total, shares);
    }

    private static MonthlyBlock monthly(List<LocalDateTime> createdAts, LocalDate today) {
        if (createdAts.size() < MONTHLY_MIN_SCHEDULES) {
            return null;
        }
        Map<java.time.YearMonth, Integer> counts = new HashMap<>();
        java.time.YearMonth first = null;
        for (LocalDateTime createdAt : createdAts) {
            java.time.YearMonth ym = java.time.YearMonth.from(createdAt);
            counts.merge(ym, 1, Integer::sum);
            if (first == null || ym.isBefore(first)) {
                first = ym;
            }
        }
        java.time.YearMonth current = java.time.YearMonth.from(today);
        java.time.YearMonth windowStart = current.minusMonths(MONTHS_SHOWN - 1);
        java.time.YearMonth start = first.isBefore(windowStart) ? windowStart : first;
        if (start.isAfter(current)) {
            start = current;
        }
        int max = 0;
        for (java.time.YearMonth ym = start; !ym.isAfter(current); ym = ym.plusMonths(1)) {
            max = Math.max(max, counts.getOrDefault(ym, 0));
        }
        List<MonthCount> months = new ArrayList<>();
        for (java.time.YearMonth ym = start; !ym.isAfter(current); ym = ym.plusMonths(1)) {
            int count = counts.getOrDefault(ym, 0);
            int bar = max == 0 ? 0 : count * 100 / max;
            months.add(new MonthCount(ym.getYear(), ym.getMonthValue(), count, bar, ym.equals(current)));
        }
        return new MonthlyBlock(createdAts.size(), months);
    }

    private static LeadTimeBlock leadTime(List<ParticipantAvailability> availabilities) {
        Map<Long, List<ParticipantAvailability>> bySchedule = new LinkedHashMap<>();
        for (ParticipantAvailability a : availabilities) {
            bySchedule.computeIfAbsent(a.scheduleId(), k -> new ArrayList<>()).add(a);
        }
        List<Integer> leads = new ArrayList<>();
        for (List<ParticipantAvailability> group : bySchedule.values()) {
            List<List<LocalDate>> picked = group.stream()
                    .map(ParticipantAvailability::availableDates)
                    .filter(dates -> !dates.isEmpty())
                    .toList();
            if (picked.size() < 2) {
                continue;
            }
            Map<LocalDate, Integer> overlap = new TreeMap<>();
            picked.forEach(dates -> dates.forEach(d -> overlap.merge(d, 1, Integer::sum)));
            LocalDate best = null;
            int bestCount = 0;
            for (Map.Entry<LocalDate, Integer> e : overlap.entrySet()) {   // 날짜 오름차순 → 동률이면 이른 날 유지
                if (e.getValue() > bestCount) {
                    best = e.getKey();
                    bestCount = e.getValue();
                }
            }
            long lead = ChronoUnit.DAYS.between(group.get(0).scheduleCreatedAt().toLocalDate(), best);
            if (lead >= 0) {
                leads.add((int) lead);
            }
        }
        if (leads.size() < LEAD_TIME_MIN_SCHEDULES) {
            return null;
        }
        int[] buckets = new int[LEAD_BUCKETS.size()];
        for (int lead : leads) {
            buckets[lead <= 6 ? 0 : lead <= 13 ? 1 : lead <= 27 ? 2 : 3]++;
        }
        int[] percents = percentages(buckets);
        List<Share> shares = IntStream.range(0, LEAD_BUCKETS.size())
                .mapToObj(i -> new Share(LEAD_BUCKETS.get(i), percents[i]))
                .toList();
        List<Integer> sorted = leads.stream().sorted().toList();
        int median = sorted.get((sorted.size() - 1) / 2);   // 짝수 개면 아래쪽 중앙값
        return new LeadTimeBlock(leads.size(), median, shares);
    }

    /** 최대 잉여 반올림 — 정수 % 합계가 정확히 100. 잉여가 같으면 앞 항목부터. */
    static int[] percentages(int[] counts) {
        int total = IntStream.of(counts).sum();
        int[] result = new int[counts.length];
        if (total == 0) {
            return result;
        }
        long[] remainders = new long[counts.length];
        int assigned = 0;
        for (int i = 0; i < counts.length; i++) {
            long scaled = (long) counts[i] * 100;
            result[i] = (int) (scaled / total);
            remainders[i] = scaled % total;
            assigned += result[i];
        }
        List<Integer> order = IntStream.range(0, counts.length).boxed()
                .sorted(Comparator.comparingLong((Integer i) -> remainders[i]).reversed()
                        .thenComparingInt(i -> i))
                .toList();
        for (int k = 0; k < 100 - assigned; k++) {
            result[order.get(k)]++;
        }
        return result;
    }
}
