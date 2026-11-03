package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttempt;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttemptRepository;
import me.singingsandhill.calendar.stock.domain.stock.EntryOutcome;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockProductType;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeMode;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
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
    private final StockRepository stockRepository = mock(StockRepository.class);

    private final StockProperties properties = new StockProperties();   // 기본 모드 PAPER

    private final DailyPerformanceReportService service = new DailyPerformanceReportService(
        positionRepository, entryAttemptRepository, mailService, stockRepository, properties);

    private StockPosition closedPosition(String code, String entry, String exit, StockCloseReason reason) {
        return closedPosition(code, entry, exit, reason, StockTradeMode.PAPER);
    }

    private StockPosition closedPosition(String code, String entry, String exit, StockCloseReason reason,
                                         StockTradeMode mode) {
        StockPosition opened = StockPosition.open(code, TODAY, new BigDecimal(entry), 10,
            new BigDecimal(entry).multiply(new BigDecimal("0.98")), new BigDecimal(entry));
        StockPosition p = StockPosition.reconstitute(null, null, code, TODAY, opened.getStatus(),
            opened.getEntryPrice(), opened.getEntryQuantity(), opened.getEntryAmount(), opened.getEnteredAt(),
            opened.getRemainingQuantity(), null, false, false, false, opened.getDayHighPrice(),
            opened.getStopLossPrice(), null, null, false, BigDecimal.ZERO, null, null, null,
            null, null, null, null, mode);
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

    /** ADR stock/algorithm/0011 — ETF·ETN 제외 판단(보류)의 근거를 매일 쌓는다. 유형 모름은 UNKNOWN. */
    @Test
    void breaksDownByProductType() {
        Stock stock = new Stock("005930", "삼성전자", TODAY);
        stock.setProductType(StockProductType.STOCK);
        Stock etf = new Stock("069500", "KODEX 200", TODAY);
        etf.setProductType(StockProductType.ETF);
        when(stockRepository.findByTradingDate(TODAY)).thenReturn(List.of(stock, etf, new Stock("088980", "맥쿼리인프라", TODAY)));
        when(positionRepository.findByTradingDate(TODAY)).thenReturn(List.of(
            closedPosition("005930", "100000", "98800", StockCloseReason.STOP_LOSS),
            closedPosition("069500", "10000", "10500", StockCloseReason.TP1)));
        when(entryAttemptRepository.findByTradingDate(TODAY)).thenReturn(List.of(
            rejected("069500", "strength"), rejected("088980", "imbalance")));

        DailyPerformanceReportService.DailyReport report = service.buildReport(TODAY);

        assertThat(report.productTypeBreakdown()).containsOnlyKeys("STOCK", "ETF", "UNKNOWN");
        DailyPerformanceReportService.ProductTypeBreakdown stocks = report.productTypeBreakdown().get("STOCK");
        assertThat(stocks.watched()).isEqualTo(1);
        assertThat(stocks.attempts()).isZero();
        assertThat(stocks.closed()).isEqualTo(1);
        assertThat(stocks.wins()).isZero();
        assertThat(stocks.realizedPnl()).isNegative();
        DailyPerformanceReportService.ProductTypeBreakdown etfs = report.productTypeBreakdown().get("ETF");
        assertThat(etfs.attempts()).isEqualTo(1);
        assertThat(etfs.wins()).isEqualTo(1);
        assertThat(etfs.realizedPnl()).isPositive();
        assertThat(report.productTypeBreakdown().get("UNKNOWN").attempts()).isEqualTo(1);
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
    void countsOrderStageEntryFailuresByLastReason() {
        // ADR stock/infrastructure/0009 — 0체결 원인(SIZE_ZERO 등)을 리포트에서 바로 본다.
        when(positionRepository.findByTradingDate(TODAY)).thenReturn(List.of());
        when(entryAttemptRepository.findByTradingDate(TODAY)).thenReturn(List.of());
        when(stockRepository.findByTradingDate(TODAY)).thenReturn(List.of(
            failedStock("111111", EntryOutcome.SIZE_ZERO), failedStock("222222", EntryOutcome.SIZE_ZERO),
            failedStock("333333", EntryOutcome.QUOTE_UNAVAILABLE),
            new Stock("444444", "무실패", TODAY)));

        DailyPerformanceReportService.DailyReport report = service.buildReport(TODAY);

        assertThat(report.entryFailureCounts()).containsEntry("SIZE_ZERO", 2L)
            .containsEntry("QUOTE_UNAVAILABLE", 1L).hasSize(2);
        assertThat(report.hasActivity()).isTrue();
    }

    private Stock failedStock(String code, EntryOutcome reason) {
        Stock stock = new Stock(code, code, TODAY);
        stock.recordEntryFailure(reason, TODAY.atTime(9, 40), 3);
        return stock;
    }

    @Test
    void reportsOnlyPositionsOfCurrentBotMode() {
        // ADR stock/observability/0003 — PAPER 실측과 LIVE 손익을 한 리포트에 합산하지 않는다.
        when(positionRepository.findByTradingDate(TODAY)).thenReturn(List.of(
            closedPosition("005930", "100000", "105000", StockCloseReason.TP1, StockTradeMode.PAPER),
            closedPosition("000660", "100000", "98800", StockCloseReason.STOP_LOSS, StockTradeMode.LIVE)));
        when(entryAttemptRepository.findByTradingDate(TODAY)).thenReturn(List.of());

        DailyPerformanceReportService.DailyReport report = service.buildReport(TODAY);

        assertThat(report.closedCount()).isEqualTo(1);
        assertThat(report.winCount()).isEqualTo(1);
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
