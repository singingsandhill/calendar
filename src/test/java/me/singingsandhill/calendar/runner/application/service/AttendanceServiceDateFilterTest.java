package me.singingsandhill.calendar.runner.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import me.singingsandhill.calendar.runner.domain.AttendanceRepository;
import me.singingsandhill.calendar.runner.domain.RunRepository;

/**
 * getAllMemberStats(from, to) 의 null 경계 정규화 검증 — RunServiceDateFilterTest 와 같은 계약.
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceDateFilterTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private RunRepository runRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    @DisplayName("from/to 둘 다 null → 기존 무필터 집계 위임")
    void bothNull_delegatesToUnfiltered() {
        attendanceService.getAllMemberStats(null, null);

        verify(attendanceRepository).findAllMemberStats();
        verify(attendanceRepository, never()).findMemberStatsByRunDateBetween(any(), any());
    }

    @Test
    @DisplayName("from 만 지정 → to 는 9999-12-31 로 정규화")
    void fromOnly_normalizesUpperBound() {
        LocalDate from = LocalDate.of(2026, 3, 1);

        attendanceService.getAllMemberStats(from, null);

        verify(attendanceRepository).findMemberStatsByRunDateBetween(from, LocalDate.of(9999, 12, 31));
    }

    @Test
    @DisplayName("to 만 지정 → from 은 EPOCH(1970-01-01)로 정규화")
    void toOnly_normalizesLowerBound() {
        LocalDate to = LocalDate.of(2026, 3, 31);

        attendanceService.getAllMemberStats(null, to);

        verify(attendanceRepository).findMemberStatsByRunDateBetween(LocalDate.EPOCH, to);
    }

    @Test
    @DisplayName("둘 다 지정 → 그대로 BETWEEN 집계 위임")
    void bothGiven_passedThrough() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        attendanceService.getAllMemberStats(from, to);

        verify(attendanceRepository).findMemberStatsByRunDateBetween(from, to);
    }
}
