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

import me.singingsandhill.calendar.runner.domain.RunRepository;

/**
 * getAllRuns(from, to) 의 null 경계 정규화 검증 — 둘 다 null 이면 기존 무필터 쿼리,
 * 한쪽만 null 이면 sentinel(EPOCH / 9999-12-31)로 채워 BETWEEN 쿼리 위임.
 */
@ExtendWith(MockitoExtension.class)
class RunServiceDateFilterTest {

    @Mock
    private RunRepository runRepository;

    @InjectMocks
    private RunService runService;

    @Test
    @DisplayName("from/to 둘 다 null → 기존 무필터 쿼리 위임")
    void bothNull_delegatesToUnfiltered() {
        runService.getAllRuns(null, null);

        verify(runRepository).findAllOrderByDateDesc();
        verify(runRepository, never()).findByDateBetweenOrderByDateDesc(any(), any());
    }

    @Test
    @DisplayName("from 만 지정 → to 는 9999-12-31 로 정규화")
    void fromOnly_normalizesUpperBound() {
        LocalDate from = LocalDate.of(2026, 2, 1);

        runService.getAllRuns(from, null);

        verify(runRepository).findByDateBetweenOrderByDateDesc(from, LocalDate.of(9999, 12, 31));
    }

    @Test
    @DisplayName("to 만 지정 → from 은 EPOCH(1970-01-01)로 정규화")
    void toOnly_normalizesLowerBound() {
        LocalDate to = LocalDate.of(2026, 2, 1);

        runService.getAllRuns(null, to);

        verify(runRepository).findByDateBetweenOrderByDateDesc(LocalDate.EPOCH, to);
    }

    @Test
    @DisplayName("둘 다 지정 → 그대로 BETWEEN 쿼리 위임")
    void bothGiven_passedThrough() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 12, 31);

        runService.getAllRuns(from, to);

        verify(runRepository).findByDateBetweenOrderByDateDesc(from, to);
    }
}
