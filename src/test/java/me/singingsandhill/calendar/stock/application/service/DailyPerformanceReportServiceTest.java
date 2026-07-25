package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttempt;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttemptRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 일일 실측 요약 (2026-07-24 리뷰 §9 / P2-5).
 *
 * PAPER 2~4주 실측으로 기대값을 판단하려면 매일의 진입·청산·손익·거절 사유가 한 곳에
 * 집계돼야 한다. 이 리포트가 LIVE 전환 판단의 근거 데이터가 된다.
 */
class DailyPerformanceReportServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 24);
    private static final BigDecimal FEE = new BigDecimal("0.00015");
    private static final BigDecimal TAX = new BigDecimal("0.0020");

    private final StockPositionRepository positionRepository = mock(StockPositionRepository.class);
    private final EntryAttemptRepository entryAttemptRepository = mock(EntryAttemptRepository.class);
    private final StockMailService mailService = mock(StockMailService.class);

    private final DailyPerformanceReportService service =
        new DailyPerformanceReportService(positionRepository, entryAttemptRepository, mailService);

    private StockPosition closedPosition(String code, String entry, String exit, StockCloseReason reason) {
        StockPosition p = StockPosition.open(code, TODAY, new BigDecimal(entry), 10,
            new BigDecimal(entry).multiply(new BigDecimal("0.98")), new BigDecimal(entry));
        p.closeRemaining(new BigDecimal(exit), reason, FEE, TAX);
        return p;
    }

    private EntryAttempt rejected(String code, String reason) {
        return EntryAttempt.of(TODAY, code, false, 1, 2, true, false, false,
            new BigDecimal("100000"), new BigDecimal("99800"), reason);
    }

    @Test
    void aggregatesWinRateAndRealizedPnl() {
        when(positionRepository.findByTradingDate(TODAY)).thenReturn(List.of(
            closedPosition("005930", "100000", "105000", StockCloseReason.TP1),  // 승
            closedPosition("000660", "100000", "98800", StockCloseReason.STOP_LOSS), // 패
            closedPosition("035420", "100000", "100500", StockCloseReason.TIME_EXIT))); // 승(소폭)
        when(entryAttemptRepository.findByTradingDate(TODAY)).thenReturn(List.of());

        DailyPerformanceReportService.DailyReport report = service.buildReport(TODAY);

        assertThat(report.closedCount()).isEqualTo(3);
        assertThat(report.winCount()).isEqualTo(2);
        assertThat(report.lossCount()).isEqualTo(1);
        assertThat(report.winRatePercent()).isEqualByComparingTo("66.67");
        // 실현손익 합계는 수수료·세금 반영값
        assertThat(report.totalRealizedPnl()).isEqualByComparingTo(
            closedPosition("005930", "100000", "105000", StockCloseReason.TP1).getRealizedPnl()
                .add(closedPosition("000660", "100000", "98800", StockCloseReason.STOP_LOSS).getRealizedPnl())
                .add(closedPosition("035420", "100000", "100500", StockCloseReason.TIME_EXIT).getRealizedPnl()));
    }

    @Test
    void groupsCloseReasonsAndRejectReasons() {
        when(positionRepository.findByTradingDate(TODAY)).thenReturn(List.of(
            closedPosition("005930", "100000", "105000", StockCloseReason.TP1),
            closedPosition("000660", "100000", "101000", StockCloseReason.TP1),
            closedPosition("035420", "100000", "98800", StockCloseReason.STOP_LOSS)));
        when(entryAttemptRepository.findByTradingDate(TODAY)).thenReturn(List.of(
            rejected("111111", "strength"), rejected("222222", "strength"),
            rejected("333333", "imbalance")));

        DailyPerformanceReportService.DailyReport report = service.buildReport(TODAY);

        assertThat(report.closeReasonCounts()).containsEntry("TP1", 2L)
            .containsEntry("STOP_LOSS", 1L);
        assertThat(report.rejectReasonCounts()).containsEntry("strength", 2L)
            .containsEntry("imbalance", 1L);
    }

    @Test
    void sendsReportWhenThereWasActivity() {
        when(positionRepository.findByTradingDate(TODAY)).thenReturn(List.of(
            closedPosition("005930", "100000", "105000", StockCloseReason.TP1)));
        when(entryAttemptRepository.findByTradingDate(TODAY)).thenReturn(List.of());

        service.sendDailyReport(TODAY);

        verify(mailService).sendDailyPerformanceReport(any());
    }

    @Test
    void skipsMailWhenNothingHappened() {
        when(positionRepository.findByTradingDate(TODAY)).thenReturn(List.of());
        when(entryAttemptRepository.findByTradingDate(TODAY)).thenReturn(List.of());

        service.sendDailyReport(TODAY);

        // 매매도 시도도 없던 날은 메일을 보내지 않는다 (알림 피로 방지)
        verify(mailService, never()).sendDailyPerformanceReport(any());
    }
}
