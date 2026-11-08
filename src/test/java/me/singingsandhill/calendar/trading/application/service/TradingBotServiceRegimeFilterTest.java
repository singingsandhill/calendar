package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.regime.MarketRegime;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
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
 * 일봉 레짐을 신호 매수 필터로 (ADR trading/risk/0007, 4-B).
 *
 * 수익 원천은 신호 매수 → 신호 매도뿐이다(SIGNAL 출구 209건 +49,539원). 레짐은 독립 매수 엔진이 아니라 신호 매수의
 * 필터로만 쓴다 — 기본 OFF(작업 6-B 결과로 결정). 진입에만 적용하고 매도·손절·수동 주문은 건드리지 않는다.
 */
class TradingBotServiceRegimeFilterTest {

    private static final String MARKET = "KRW-ADA";

    private final BithumbApiClient api = mock(BithumbApiClient.class);
    private final TradeRepository tradeRepo = mock(TradeRepository.class);
    private final PositionRepository posRepo = mock(PositionRepository.class);
    private final TradingCircuitBreaker breaker = mock(TradingCircuitBreaker.class);
    private final AccountSnapshotRepository snapRepo = mock(AccountSnapshotRepository.class);
    private final IndicatorService indicators = mock(IndicatorService.class);
    private final RiskManagementService risk = mock(RiskManagementService.class);
    private final DailyRegimeService regime = mock(DailyRegimeService.class);
    private final TradingEventService events = mock(TradingEventService.class);
    private TradingProperties props;

    @BeforeEach
    void setUp() {
        props = new TradingProperties();
        when(breaker.isEntryBlocked(any(), any())).thenReturn(false);
        when(snapRepo.findFirstByMarketAndDateRange(any(), any(), any())).thenReturn(Optional.empty());
        when(posRepo.findByMarketAndStatusAndClosedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        when(api.getKrwBalance()).thenReturn(new BithumbAccountResponse("KRW", "1000000", "0", null, null, "KRW"));
        when(api.getCoinBalance()).thenReturn(new BithumbAccountResponse("ADA", "0", "0", null, null, "KRW"));
        when(api.getCurrentPrice()).thenReturn(1000.0);
        when(posRepo.findByMarketAndStatus(any(), any())).thenReturn(List.of());
        when(indicators.calculateATRPercent(any())).thenReturn(null);   // 기본 비중 0.25
        BithumbOrderResponse.TradeDetail fill = new BithumbOrderResponse.TradeDetail(
            MARKET, "t", "1000", "248", "248000", "bid", null);
        when(api.placeMarketBuyOrder(any())).thenReturn(new BithumbOrderResponse("u-1", "bid", "price", "1000",
            "done", MARKET, null, "248", "0", "0", "0", "0", "0", "248", 1, List.of(fill)));
        when(risk.calculateStopLossPrice(any())).thenReturn(new BigDecimal("985"));
        when(risk.calculateTakeProfitPrice(any())).thenReturn(new BigDecimal("1030"));
    }

    private TradingBotService service() {
        return new TradingBotService(null, null, indicators, risk, mock(RebalanceService.class), api, tradeRepo,
            posRepo, props, events, breaker, snapRepo, mock(PlatformTransactionManager.class), regime);
    }

    private Signal buySignal() {
        Signal signal = mock(Signal.class);
        when(signal.getTotalScore()).thenReturn(50);
        return signal;
    }

    @Test
    void filterOffNeverConsultsRegime() {
        service().executeBuy(MARKET, buySignal());

        verify(regime, never()).currentRegime();
        verify(api).placeMarketBuyOrder(any());
    }

    @Test
    void blockBearStopsSignalBuyInBearRegime() {
        props.getRegime().setFilterMode(TradingProperties.Regime.FilterMode.BLOCK_BEAR);
        when(regime.currentRegime()).thenReturn(MarketRegime.BEAR);

        service().executeBuy(MARKET, buySignal());

        verify(api, never()).placeMarketBuyOrder(any());
        verify(events).record(eq(TradingEventLevel.NOTICE), eq("REGIME_FILTER_BLOCKED"), eq(MARKET), anyString());
    }

    @Test
    void blockBearAllowsBuyInBullRegime() {
        props.getRegime().setFilterMode(TradingProperties.Regime.FilterMode.BLOCK_BEAR);
        when(regime.currentRegime()).thenReturn(MarketRegime.BULL);

        service().executeBuy(MARKET, buySignal());

        verify(api).placeMarketBuyOrder(any());
    }

    @Test
    void scaleBearHalvesOrderRatio() {
        props.getRegime().setFilterMode(TradingProperties.Regime.FilterMode.SCALE_BEAR);
        when(regime.currentRegime()).thenReturn(MarketRegime.BEAR);

        service().executeBuy(MARKET, buySignal());

        ArgumentCaptor<BigDecimal> amount = ArgumentCaptor.forClass(BigDecimal.class);
        verify(api).placeMarketBuyOrder(amount.capture());
        // 1,000,000 × 0.25 × 0.5 = 125,000 → 슬리피지 버퍼 0.5% → 124,375
        assertThat(amount.getValue()).isEqualByComparingTo("124375");
        ArgumentCaptor<Trade> trade = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo).save(trade.capture());
        assertThat(trade.getValue().getOrderRatio()).isEqualByComparingTo("0.125");
        verify(events).record(eq(TradingEventLevel.NOTICE), eq("REGIME_FILTER_SCALED"), eq(MARKET), anyString());
    }

    @Test
    void unknownRegimeBlocksByDefaultWhenFilterIsOn() {
        props.getRegime().setFilterMode(TradingProperties.Regime.FilterMode.SCALE_BEAR);
        when(regime.currentRegime()).thenReturn(MarketRegime.UNKNOWN);

        service().executeBuy(MARKET, buySignal());

        verify(api, never()).placeMarketBuyOrder(any());
    }

    @Test
    void unknownRegimeCanBeAllowed() {
        props.getRegime().setFilterMode(TradingProperties.Regime.FilterMode.BLOCK_BEAR);
        props.getRegime().setUnknownPolicy(TradingProperties.Regime.UnknownPolicy.ALLOW);
        when(regime.currentRegime()).thenReturn(MarketRegime.UNKNOWN);

        service().executeBuy(MARKET, buySignal());

        ArgumentCaptor<BigDecimal> amount = ArgumentCaptor.forClass(BigDecimal.class);
        verify(api).placeMarketBuyOrder(amount.capture());
        assertThat(amount.getValue()).isEqualByComparingTo("248750");   // 축소 없음
    }

    @Test
    void repeatedBlocksAreSummarizedOncePerHour() {
        props.getRegime().setFilterMode(TradingProperties.Regime.FilterMode.BLOCK_BEAR);
        when(regime.currentRegime()).thenReturn(MarketRegime.BEAR);
        TradingBotService svc = service();

        svc.executeBuy(MARKET, buySignal());
        svc.executeBuy(MARKET, buySignal());
        svc.executeBuy(MARKET, buySignal());

        verify(events, times(1)).record(eq(TradingEventLevel.NOTICE), eq("REGIME_FILTER_BLOCKED"), eq(MARKET),
            anyString());
    }

    @Test
    void manualBuyIsNotFiltered() {
        props.getBot().setEnabled(true);
        props.getRegime().setFilterMode(TradingProperties.Regime.FilterMode.BLOCK_BEAR);
        when(regime.currentRegime()).thenReturn(MarketRegime.BEAR);

        boolean ok = service().manualBuy(new BigDecimal("50000"));

        assertThat(ok).isTrue();
        verify(regime, never()).currentRegime();
    }
}
