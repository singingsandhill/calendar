package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.EntrySource;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.regime.MarketRegime;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.application.dto.IndicatorResult;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 리밸런싱 엔진 — 일봉 레짐 + 켜졌을 때의 구멍 봉인 (ADR trading/risk/0007, 4-C).
 *
 * 1분봉 MA60 레짐은 10/11~10/20 목표 비중을 70%↔30% 로 매번 뒤집었다. 구멍 1: 약세 전환 뒤에도 '수익일 때만 매도' 가드가
 * 손실 중인 70% 를 비상 손절까지 들고 간다(10/11 밤 스킵 162회). 구멍 2: 비상 손절 뒤 비중 0 → 편차 초과 → 재매수로
 * -15% 단위 루프가 되살아난다.
 */
class RebalanceServiceRegimeGuardTest {

    private static final String MARKET = "KRW-ADA";
    private static final LocalDate TODAY = LocalDate.now();

    private final BithumbApiClient api = mock(BithumbApiClient.class);
    private final PositionRepository posRepo = mock(PositionRepository.class);
    private final RiskManagementService risk = mock(RiskManagementService.class);
    private final DailyRegimeService regime = mock(DailyRegimeService.class);
    private final TradingEventService events = mock(TradingEventService.class);
    private final IndicatorService indicators = mock(IndicatorService.class);
    private TradingProperties props;

    @BeforeEach
    void setUp() {
        props = new TradingProperties();
        props.getRebalancing().setEnabled(true);
        when(posRepo.findLatestClosedByMarketAndCloseReason(eq(MARKET), eq(CloseReason.CATASTROPHIC_STOP)))
            .thenReturn(Optional.empty());
        when(risk.calculateStopLossPrice(any())).thenReturn(new BigDecimal("985"));
        when(risk.calculateTakeProfitPrice(any())).thenReturn(new BigDecimal("1030"));
    }

    private RebalanceService service() {
        return new RebalanceService(api, indicators, props, mock(TradeRepository.class), posRepo, events, risk,
            mock(PlatformTransactionManager.class), regime);
    }

    private void balances(String krw, String coin, double price) {
        when(api.getKrwBalance()).thenReturn(new BithumbAccountResponse("KRW", krw, "0", null, null, "KRW"));
        when(api.getCoinBalance()).thenReturn(new BithumbAccountResponse("ADA", coin, "0", null, null, "KRW"));
        when(api.getCurrentPrice()).thenReturn(price);
    }

    private BithumbOrderResponse filled(String price, String volume) {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
            MARKET, "t", price, volume, "0", "bid", null);
        return new BithumbOrderResponse("u", "bid", "price", price, "done", MARKET, null,
            volume, "0", "0", "0", "0", "0", volume, 1, List.of(t));
    }

    private Position open(EntrySource source, String entry, LocalDateTime openedAt) {
        BigDecimal ep = new BigDecimal(entry);
        Position p = new Position(null, MARKET, PositionStatus.OPEN, ep, BigDecimal.TEN, ep.multiply(BigDecimal.TEN),
            null, null, null, null, null, null, null, null, ep, false, null, openedAt, null, openedAt,
            BigDecimal.ZERO, null, null);
        p.setEntrySource(source);
        return p;
    }

    @Test
    void dailyBullRegimeTargetsBullRatio() {
        balances("10000", "0", 1000);
        when(regime.currentRegime()).thenReturn(MarketRegime.BULL);
        when(api.placeMarketBuyOrder(any())).thenReturn(filled("1000", "6.965"));

        RebalanceService.RebalanceResult result = service().checkAndExecute(MARKET);

        assertThat(result.targetRatio()).isEqualByComparingTo("0.70");
        verify(api).placeMarketBuyOrder(any());
        verify(indicators, never()).calculate(anyString());   // 1분봉 MA60 은 레거시 옵션에서만
    }

    @Test
    void unknownDailyRegimeSkipsRebalancing() {
        balances("10000", "0", 1000);
        when(regime.currentRegime()).thenReturn(MarketRegime.UNKNOWN);

        RebalanceService.RebalanceResult result = service().checkAndExecute(MARKET);

        assertThat(result.executed()).isFalse();
        verify(api, never()).placeMarketBuyOrder(any());
    }

    @Test
    void legacyMinuteSourceStillUsesMa60() {
        props.getRebalancing().setRegimeSource(TradingProperties.Rebalancing.RegimeSource.MINUTE_MA60);
        balances("10000", "0", 1000);
        IndicatorResult bull = mock(IndicatorResult.class);
        when(bull.ma60()).thenReturn(new BigDecimal("990"));
        when(bull.isPriceAboveMa60()).thenReturn(true);
        when(indicators.calculate(MARKET)).thenReturn(bull);
        when(api.placeMarketBuyOrder(any())).thenReturn(filled("1000", "6.965"));

        RebalanceService.RebalanceResult result = service().checkAndExecute(MARKET);

        assertThat(result.targetRatio()).isEqualByComparingTo("0.70");
        verify(regime, never()).currentRegime();
    }

    @Test
    void regimeSwitchSellClosesRebalancePositionsAtALossFirstButKeepsGuardForSignalPositions() {
        // 약세 전환(BULL 에서 산 리밸런싱 포지션) — 손실이어도 REBALANCE 부터 FIFO 청산, 신호 포지션은 가드 유지
        Position olderSignal = open(EntrySource.SIGNAL, "1200", LocalDateTime.now().minusDays(3));
        Position rebalance = open(EntrySource.REBALANCE, "1150", LocalDateTime.now().minusDays(2));
        when(posRepo.findByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(List.of(olderSignal, rebalance));
        when(regime.currentRegime()).thenReturn(MarketRegime.BEAR);
        when(regime.regimeInEffectAt(rebalance.getOpenedAt())).thenReturn(MarketRegime.BULL);
        when(regime.regimeInEffectAt(olderSignal.getOpenedAt())).thenReturn(MarketRegime.BULL);

        BigDecimal sold = service().sellByClosingProfitablePositions(MARKET, new BigDecimal("20"), new BigDecimal("1100"));

        verify(risk).closePosition(eq(rebalance), any(), eq(CloseReason.REBALANCE));
        verify(risk, never()).closePosition(eq(olderSignal), any(), any());
        assertThat(sold).isEqualByComparingTo("10");
    }

    @Test
    void sameRegimeDeviationSellKeepsProfitGuard() {
        Position rebalance = open(EntrySource.REBALANCE, "1150", LocalDateTime.now().minusDays(1));
        when(posRepo.findByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(List.of(rebalance));
        when(regime.currentRegime()).thenReturn(MarketRegime.BEAR);
        when(regime.regimeInEffectAt(rebalance.getOpenedAt())).thenReturn(MarketRegime.BEAR);

        BigDecimal sold = service().sellByClosingProfitablePositions(MARKET, new BigDecimal("10"), new BigDecimal("1100"));

        verify(risk, never()).closePosition(any(), any(), any());
        assertThat(sold).isEqualByComparingTo("0");
    }

    @Test
    void catastrophicStopBlocksRebalanceBuyUntilRegimeChanges() {
        Position stopped = open(EntrySource.REBALANCE, "1000", LocalDateTime.now().minusDays(5));
        stopped.close(new BigDecimal("840"), BigDecimal.TEN, CloseReason.CATASTROPHIC_STOP);
        when(posRepo.findLatestClosedByMarketAndCloseReason(MARKET, CloseReason.CATASTROPHIC_STOP))
            .thenReturn(Optional.of(stopped));
        when(regime.hasRegimeChangedAfter(stopped.getClosedAt())).thenReturn(false);
        balances("100000", "0", 1000);
        when(regime.currentRegime()).thenReturn(MarketRegime.BEAR);   // 비중 0 → BEAR 목표 30% 로 매수 신호(30,000원)

        RebalanceService.RebalanceResult result = service().checkAndExecute(MARKET);

        assertThat(result.executed()).isFalse();
        verify(api, never()).placeMarketBuyOrder(any());
        verify(events).record(eq(TradingEventLevel.NOTICE), eq("REBALANCE_SKIPPED"), eq(MARKET), anyString());
    }

    @Test
    void rebalanceBuyResumesAfterRegimeChange() {
        Position stopped = open(EntrySource.REBALANCE, "1000", LocalDateTime.now().minusDays(5));
        stopped.close(new BigDecimal("840"), BigDecimal.TEN, CloseReason.CATASTROPHIC_STOP);
        when(posRepo.findLatestClosedByMarketAndCloseReason(MARKET, CloseReason.CATASTROPHIC_STOP))
            .thenReturn(Optional.of(stopped));
        when(regime.hasRegimeChangedAfter(stopped.getClosedAt())).thenReturn(true);
        balances("10000", "0", 1000);
        when(regime.currentRegime()).thenReturn(MarketRegime.BULL);
        when(api.placeMarketBuyOrder(any())).thenReturn(filled("1000", "6.965"));

        service().checkAndExecute(MARKET);

        verify(api).placeMarketBuyOrder(any());
    }

    @Test
    void repeatedSkipsWithSameReasonAreRecordedOncePerHour() {
        when(posRepo.findByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(List.of(
            open(EntrySource.REBALANCE, "1500", LocalDateTime.now().minusDays(1))));
        when(regime.regimeInEffectAt(any())).thenReturn(MarketRegime.BULL);
        when(regime.currentRegime()).thenReturn(MarketRegime.BULL);
        balances("0", "100", 1000);   // 코인 100% → 목표 70% 로 30,000원 매도 필요, 그러나 손실 포지션뿐(같은 레짐)
        props.getRebalancing().setCooldownMinutes(0);
        RebalanceService svc = service();

        svc.checkAndExecute(MARKET);
        svc.checkAndExecute(MARKET);

        verify(events, times(1)).record(eq(TradingEventLevel.NOTICE), eq("REBALANCE_SKIPPED"), eq(MARKET), anyString());
    }
}
