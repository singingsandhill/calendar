package me.singingsandhill.calendar.datedate.domain.participant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import me.singingsandhill.calendar.datedate.domain.schedule.YearMonth;

/**
 * 모임 데이터 리포트용 읽기 모델 — 참여자 한 명의 고른 날짜와 그 일정의 연월·주 모드·생성 시각
 * (ADR datedate/domain/0013). 이름·ID 는 담지 않는다.
 */
public record ParticipantAvailability(Long scheduleId, int year, int month, int weeks,
                                      LocalDateTime scheduleCreatedAt, List<Integer> selections) {

    /** 고른 날짜를 실제 날짜로 — null·범위 밖 인덱스는 건너뛰고 중복은 한 번만 센다. */
    public List<LocalDate> availableDates() {
        if (selections == null || selections.isEmpty()) {
            return List.of();
        }
        YearMonth ym = new YearMonth(year, month);
        return selections.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(index -> ym.dateOf(index, weeks))
                .flatMap(Optional::stream)
                .toList();
    }
}
