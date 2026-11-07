package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.account.DailySummary;
import me.singingsandhill.calendar.trading.domain.account.DailySummaryRepository;
import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeMode;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 손익 집계의 운영 모드 필터 (ADR trading/observability/0004).
 *
 * PAPER- 체결 44건이 LIVE 손익·승률에 합산되고 있었다. 기본은 LIVE, NULL(백필 전 레거시)은 LIVE 로 본다.
 */
class ProfitServiceModeFilterTest {

    private static final String MARKET = "KRW-ADA";

    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final AccountSnapshotRepository snapshotRepository = mock(AccountSnapshotRepository.class);
    private final DailySummaryRepository dailySummaryRepository = mock(DailySummaryRepository.class);
    private final BithumbApiClient api = mock(BithumbApiClient.class);

    private final ProfitService service = new ProfitService(positionRepository, tradeRepository,
        snapshotRepository, dailySummaryRepository, api, new TradingProperties());

    private static Position closed(String exit, TradeMode mode) {
        Position p = Position.open(MARKET, new BigDecimal("1000"), BigDecimal.TEN,
            new BigDecimal("985"), new BigDecimal("1030"));
        p.close(new BigDecimal(exit), BigDecimal.TEN, CloseReason.SIGNAL);
        p.setMode(mode);
        return p;
    }

    @Test
    void profitSummaryDefaultsToLiveAndTreatsNullAsLegacyLive() {
        when(positionRepository.findOpenPositionsByMarket(MARKET)).thenReturn(List.of());
        when(positionRepository.findByMarketAndStatus(MARKET, PositionStatus.CLOSED)).thenReturn(List.of(
            closed("1100", TradeMode.LIVE),     // +1,000
            closed("900", TradeMode.PAPER),     // -1,000 (제외)
            closed("1050", null)));             // +500 (백필 전 레거시 → LIVE)

        ProfitService.ProfitSummary summary = service.getProfitSummary();

        assertThat(summary.totalTrades()).isEqualTo(2);
        assertThat(summary.realizedPnl()).isEqualByComparingTo("1500");
        assertThat(summary.winRate()).isEqualTo(100.0);
    }

    @Test
    void profitSummaryCanBeAskedForPaper() {
        when(positionRepository.findOpenPositionsByMarket(MARKET)).thenReturn(List.of());
        when(positionRepository.findByMarketAndStatus(MARKET, PositionStatus.CLOSED)).thenReturn(List.of(
            closed("1100", TradeMode.LIVE), closed("900", TradeMode.PAPER), closed("1050", null)));

        ProfitService.ProfitSummary summary = service.getProfitSummary(TradeMode.PAPER);

        assertThat(summary.totalTrades()).isEqualTo(1);
        assertThat(summary.realizedPnl()).isEqualByComparingTo("-1000");
    }

    @Test
    void dailySummaryCountsLivePositionsOnly() {
        LocalDate day = LocalDate.of(2026, 10, 21);
        when(positionRepository.findByMarketAndStatusAndClosedAtBetween(eq(MARKET), eq(PositionStatus.CLOSED),
            any(), any())).thenReturn(List.of(closed("1100", TradeMode.LIVE), closed("900", TradeMode.PAPER)));
        when(snapshotRepository.findFirstByMarketAndDateRange(any(), any(), any())).thenReturn(Optional.empty());
        when(snapshotRepository.findLastByMarketAndDateRange(any(), any(), any())).thenReturn(Optional.empty());

        service.generateDailySummary(day);

        ArgumentCaptor<DailySummary> captor = ArgumentCaptor.forClass(DailySummary.class);
        verify(dailySummaryRepository).save(captor.capture());
        // PAPER 손실(-1,000)이 빠져 LIVE 이익만 남는다
        assertThat(captor.getValue().getRealizedPnl()).isEqualByComparingTo("1000");
        assertThat(captor.getValue().getWinCount()).isEqualTo(1);
        assertThat(captor.getValue().getLoseCount()).isZero();
    }

    @Test
    void todaySummaryExcludesPaperTradesAndPositions() {
        when(positionRepository.findByMarketAndStatusAndClosedAtBetween(eq(MARKET), eq(PositionStatus.CLOSED),
            any(), any())).thenReturn(List.of(closed("1100", TradeMode.LIVE), closed("900", TradeMode.PAPER)));
        Trade liveTrade = Trade.createBuyOrder("u-1", MARKET, new BigDecimal("1000"), BigDecimal.TEN,
            "market", null, "x");
        liveTrade.markExecuted(new BigDecimal("1000"), BigDecimal.TEN, BigDecimal.ZERO);
        liveTrade.setMode(TradeMode.LIVE);
        Trade paperTrade = Trade.createBuyOrder("PAPER-1", MARKET, new BigDecimal("1000"), BigDecimal.TEN,
            "market", null, "x");
        paperTrade.markExecuted(new BigDecimal("1000"), BigDecimal.TEN, BigDecimal.ZERO);
        paperTrade.setMode(TradeMode.PAPER);
        when(tradeRepository.findByMarketAndCreatedAtBetween(eq(MARKET), any(), any()))
            .thenReturn(List.of(liveTrade, paperTrade));
        when(positionRepository.findByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(List.of());

        ProfitService.TodaySummary today = service.getTodaySummary();

        assertThat(today.closedPositions()).isEqualTo(1);
        assertThat(today.realizedPnl()).isEqualByComparingTo("1000");
        assertThat(today.doneTrades()).isEqualTo(1);
    }
}
