package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.MonthCount;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.Share;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantAvailability;

/** 모임 데이터 리포트 계산 (ADR datedate/domain/0013). 입력은 손으로 만들고 기대값도 손으로 계산했다. */
class MeetupReportCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    private static final LocalDateTime OCT_1 = LocalDateTime.of(2026, 10, 1, 10, 0);

    /** 2026-10 기존 모드(weeks 5) — 인덱스 = 10월 날짜. 10-01 목, 02 금, 03 토, 04 일, 10 토. */
    private static ParticipantAvailability oct(long scheduleId, LocalDateTime created, Integer... days) {
        return new ParticipantAvailability(scheduleId, 2026, 10, 5, created, List.of(days));
    }

    private static List<ParticipantAvailability> weekdayFixture(int participants) {
        List<ParticipantAvailability> list = new ArrayList<>();
        for (int i = 0; i < participants; i++) {
            list.add(oct(i, OCT_1, 1, 2, 3, 4, 10));   // 목1 금1 토2 일1
        }
        return list;
    }

    @Test
    @DisplayName("요일 비율 — 60명 × 5날짜 = 300: 일20 목20 금20 토40 (기존 모드 인덱스 = 그 달 날짜)")
    void weekdayShares() {
        MeetupReportDto r = MeetupReportCalculator.calculate(weekdayFixture(60), List.of(), TODAY);

        assertThat(r.weekdays().sampleSize()).isEqualTo(300);
        assertThat(r.weekdays().shares()).containsExactly(
                new Share("SUNDAY", 20), new Share("MONDAY", 0), new Share("TUESDAY", 0),
                new Share("WEDNESDAY", 0), new Share("THURSDAY", 20), new Share("FRIDAY", 20),
                new Share("SATURDAY", 40));
    }

    @Test
    @DisplayName("요일 비율 — 두 모드 혼합: 확장 모드(weeks 7) 인덱스 1~5 는 09-27(일)~10-01(목), 그 달 밖 날짜도 센다")
    void weekdaySharesMixedModes() {
        List<ParticipantAvailability> list = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            list.add(oct(i, OCT_1, 1, 2, 3, 4, 10));   // 기존 모드: 목1 금1 토2 일1
            list.add(new ParticipantAvailability(100L + i, 2026, 10, 7, OCT_1, List.of(1, 2, 3, 4, 5)));   // 일 월 화 수 목
        }

        MeetupReportDto r = MeetupReportCalculator.calculate(list, List.of(), TODAY);

        assertThat(r.weekdays().sampleSize()).isEqualTo(300);
        assertThat(r.weekdays().shares()).containsExactly(
                new Share("SUNDAY", 20), new Share("MONDAY", 10), new Share("TUESDAY", 10),
                new Share("WEDNESDAY", 10), new Share("THURSDAY", 20), new Share("FRIDAY", 10),
                new Share("SATURDAY", 20));
    }

    @Test
    @DisplayName("요일 비율 — 선택 날짜 295개(< 300)면 블록 없음")
    void weekdayBelowThreshold() {
        assertThat(MeetupReportCalculator.calculate(weekdayFixture(59), List.of(), TODAY).weekdays()).isNull();
    }

    @Test
    @DisplayName("월별 — 08월 10개, 09월 0개, 10월 20개: 빈 달도 0 으로, 이번 달은 진행 중, 막대는 최댓값 대비")
    void monthlyCountsIncludeEmptyMonth() {
        List<LocalDateTime> created = new ArrayList<>();
        for (int i = 0; i < 10; i++) created.add(LocalDateTime.of(2026, 8, 5, 12, 0));
        for (int i = 0; i < 20; i++) created.add(LocalDateTime.of(2026, 10, 2, 12, 0));

        MeetupReportDto r = MeetupReportCalculator.calculate(List.of(), created, TODAY);

        assertThat(r.monthly().total()).isEqualTo(30);
        assertThat(r.monthly().months()).containsExactly(
                new MonthCount(2026, 8, 10, 50, false),
                new MonthCount(2026, 9, 0, 0, false),
                new MonthCount(2026, 10, 20, 100, true));
    }

    @Test
    @DisplayName("월별 — 최근 12개월만: 2025-01 일정이 있어도 2025-11 부터 2026-10 까지 12칸")
    void monthlyWindowIsTwelveMonths() {
        List<LocalDateTime> created = new ArrayList<>();
        created.add(LocalDateTime.of(2025, 1, 10, 9, 0));
        for (int i = 0; i < 29; i++) created.add(LocalDateTime.of(2026, 10, 3, 9, 0));

        MeetupReportDto r = MeetupReportCalculator.calculate(List.of(), created, TODAY);

        assertThat(r.monthly().months()).hasSize(12);
        assertThat(r.monthly().months().get(0)).isEqualTo(new MonthCount(2025, 11, 0, 0, false));
        assertThat(r.monthly().months().get(11)).isEqualTo(new MonthCount(2026, 10, 29, 100, true));
    }

    @Test
    @DisplayName("월별 — 일정 29개(< 30)면 블록 없음")
    void monthlyBelowThreshold() {
        List<LocalDateTime> created = new ArrayList<>();
        for (int i = 0; i < 29; i++) created.add(OCT_1);
        assertThat(MeetupReportCalculator.calculate(List.of(), created, TODAY).monthly()).isNull();
    }

    /** 일정 n 개, 각 2명이 같은 날(day) 하나를 고른다 → 최다 겹침일 = day. */
    private static void addSchedules(List<ParticipantAvailability> list, long fromId, int n, LocalDateTime created, int day) {
        for (long id = fromId; id < fromId + n; id++) {
            list.add(oct(id, created, day));
            list.add(oct(id, created, day));
        }
    }

    @Test
    @DisplayName("리드타임 — 2일×10, 9일×10, 19일×6, 30일×4: 구간 34/33/20/13(최대 잉여, 동률은 앞 구간), 아래쪽 중앙값 9")
    void leadTimeBucketsAndMedian() {
        List<ParticipantAvailability> list = new ArrayList<>();
        addSchedules(list, 0, 10, OCT_1, 3);    // 10-03 − 10-01 = 2
        addSchedules(list, 10, 10, OCT_1, 10);  // 9
        addSchedules(list, 20, 6, OCT_1, 20);   // 19
        addSchedules(list, 26, 4, OCT_1, 31);   // 30

        MeetupReportDto.LeadTimeBlock lead = MeetupReportCalculator.calculate(list, List.of(), TODAY).leadTime();

        assertThat(lead.sampleSize()).isEqualTo(30);
        assertThat(lead.medianDays()).isEqualTo(9);   // 정렬 후 인덱스 (30−1)/2 = 14
        assertThat(lead.buckets()).containsExactly(
                new Share("D0_6", 34), new Share("D7_13", 33), new Share("D14_27", 20), new Share("D28_PLUS", 13));
    }

    @Test
    @DisplayName("리드타임 — 최다 겹침이 동률이면 이른 날, 당일(0일)은 0~6일, 생성일 이전·1인·빈 선택 일정은 제외")
    void leadTimeTieAndExclusions() {
        List<ParticipantAvailability> list = new ArrayList<>();
        for (long id = 0; id < 30; id++) {
            list.add(oct(id, OCT_1, 1, 5));   // 1일·5일 동률 2명 → 이른 1일 → 0일
            list.add(oct(id, OCT_1, 5, 1));
        }
        LocalDateTime oct20 = LocalDateTime.of(2026, 10, 20, 9, 0);
        list.add(oct(100, oct20, 3));        // 03 − 20 = −17 → 제외
        list.add(oct(100, oct20, 3));
        list.add(oct(101, OCT_1, 3));        // 1명뿐 → 제외
        list.add(oct(102, OCT_1, 3));        // 한 명은 빈 선택 → 날짜를 고른 사람 1명 → 제외
        list.add(new ParticipantAvailability(102L, 2026, 10, 5, OCT_1, List.of()));

        MeetupReportDto.LeadTimeBlock lead = MeetupReportCalculator.calculate(list, List.of(), TODAY).leadTime();

        assertThat(lead.sampleSize()).isEqualTo(30);
        assertThat(lead.medianDays()).isZero();
        assertThat(lead.buckets()).containsExactly(
                new Share("D0_6", 100), new Share("D7_13", 0), new Share("D14_27", 0), new Share("D28_PLUS", 0));
    }

    @Test
    @DisplayName("리드타임 — 해당 일정 29개(< 30)면 블록 없음")
    void leadTimeBelowThreshold() {
        List<ParticipantAvailability> list = new ArrayList<>();
        addSchedules(list, 0, 29, OCT_1, 3);
        assertThat(MeetupReportCalculator.calculate(list, List.of(), TODAY).leadTime()).isNull();
    }

    @Test
    @DisplayName("기준일은 넘겨받은 오늘")
    void asOfIsToday() {
        assertThat(MeetupReportCalculator.calculate(List.of(), List.of(), TODAY).asOf()).isEqualTo(TODAY);
    }
}
