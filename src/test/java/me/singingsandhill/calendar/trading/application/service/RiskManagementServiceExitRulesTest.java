package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.signal.SignalRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 출구 규칙 — 틱 하한·ATR 손절·시간청산 모드 (ADR trading/strategy/0011).
 *
 * ADA ≈ 290원, 1틱 = 0.34%: 트레일링 0.8% = 2.4틱, 손절 1.5% = 4.4틱(실체결 평균 -1.77%). 7월 이후 시간청산 74건은
 * 이익 포지션을 6시간에 +0.60% 로 끊었다. 진입 290원 × 10개, 진입수수료 0, 수수료율 0.25%(판정은 수수료차감 손익률).
 */
class RiskManagementServiceExitRulesTest {

    private static final String MARKET = "KRW-ADA";

    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final BithumbApiClient api = mock(BithumbApiClient.class);
    private final IndicatorService indicators = mock(IndicatorService.class);
    private final SignalRepository signals = mock(SignalRepository.class);
    private final TradingProperties props = baseProps();

    private static TradingProperties baseProps() {
        TradingProperties props = new TradingProperties();
        props.getRisk().setStopLoss(-0.015);
        props.getRisk().setTakeProfit(0.03);
        props.getRisk().setTrailingStop(0.008);
        props.getRisk().setTrailingActivation(0.015);
        props.getRisk().setTakerFeeRate(0.0025);
        return props;
    }

    private RiskManagementService service() {
        return new RiskManagementService(positionRepository, mock(TradeRepository.class), api, props,
            mock(TradingEventService.class), mock(TradingCircuitBreaker.class), mock(PlatformTransactionManager.class),
            indicators, signals);
    }

    private Position position(LocalDateTime openedAt, BigDecimal hwm, boolean trailingActive) {
        return new Position(1L, MARKET, PositionStatus.OPEN,
            new BigDecimal("290"), BigDecimal.TEN, new BigDecimal("2900"),
            null, null, null, null, null, null, null,
            null, hwm, trailingActive, null, openedAt, null, openedAt, BigDecimal.ZERO, null, null);
    }

    private Position fresh() {
        return position(LocalDateTime.now(), new BigDecimal("290"), false);
    }

    private Position aged() {
        return position(LocalDateTime.now().minusHours(7), new BigDecimal("290"), false);
    }

    private CloseReason tick(Position pos, double price) {
        when(positionRepository.findByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(List.of(pos));
        when(api.getCurrentPrice()).thenReturn(price);
        return service().checkAndExecuteRiskRules(MARKET);
    }

    // ===== 틱 하한 (기본 ON, 5틱) =====

    @Test
    void stopTickFloorHoldsInsideFiveTicks() {
        // 286: (2,860 - 2,900 - 8) / 2,900 = -1.655% — 종전 -1.5% 손절 구간이지만 5틱 하한 -1.724% 안
        assertThat(tick(fresh(), 286.0)).isNull();
        verify(api, never()).placeMarketSellOrder(any());
    }

    @Test
    void stopTickFloorStillStopsBeyondFiveTicks() {
        // 284: -2.34%
        assertThat(tick(fresh(), 284.0)).isEqualTo(CloseReason.STOP_LOSS);
    }

    @Test
    void zeroStopTicksRestoresPercentStop() {
        props.getRisk().setMinStopTicks(0);

        assertThat(tick(fresh(), 286.0)).isEqualTo(CloseReason.STOP_LOSS);
    }

    @Test
    void trailingTickFloorWidensTrailFromHighWaterMark() {
        // HWM 300, 트레일링 폭 = max(300 × 0.8% = 2.4원, 5틱) = 5원 → 스탑 295 (종전 297)
        Position pos = position(LocalDateTime.now(), new BigDecimal("300"), true);

        CloseReason reason = tick(pos, 296.0);

        assertThat(reason).isNull();
        assertThat(pos.getTrailingStopPrice()).isEqualByComparingTo("295");
    }

    @Test
    void zeroTrailingTicksRestoresNarrowTrail() {
        props.getRisk().setMinTrailingTicks(0);
        Position pos = position(LocalDateTime.now(), new BigDecimal("300"), true);

        assertThat(tick(pos, 296.0)).isEqualTo(CloseReason.TRAILING_STOP);   // 스탑 297
    }

    // ===== ATR 손절 모드 =====

    @Test
    void atrStopModeUsesAtrMultiple() {
        props.getRisk().setStopMode(TradingProperties.Risk.StopMode.ATR);
        props.getRisk().setAtrMultiplier(2.0);
        when(indicators.calculateAggregatedATRPercent(MARKET, 15, 14)).thenReturn(new BigDecimal("1.5"));

        assertThat(tick(fresh(), 284.0)).isNull();                          // -2.34% > -3%
        assertThat(tick(fresh(), 280.0)).isEqualTo(CloseReason.STOP_LOSS);  // -3.69%
    }

    @Test
    void atrStopModeFallsBackToPercentWhenAtrUnavailable() {
        props.getRisk().setStopMode(TradingProperties.Risk.StopMode.ATR);
        when(indicators.calculateAggregatedATRPercent(MARKET, 15, 14)).thenReturn(null);

        assertThat(tick(fresh(), 284.0)).isEqualTo(CloseReason.STOP_LOSS);
    }

    // ===== 시간청산 모드 =====

    @Test
    void breakevenCloseIsTheDefaultTimeExit() {
        // 295: (2,950 - 2,900 - 8) / 2,900 = +1.448%, 보유 7시간 ≥ 360분
        assertThat(tick(aged(), 295.0)).isEqualTo(CloseReason.TIME_EXIT);
    }

    @Test
    void tightenTrailActivatesTrailingInsteadOfClosing() {
        props.getBot().setTimeExitMode(TradingProperties.Bot.TimeExitMode.TIGHTEN_TRAIL);
        Position pos = aged();

        CloseReason reason = tick(pos, 295.0);

        assertThat(reason).isNull();
        assertThat(pos.isTrailingStopActive()).isTrue();
        // max(손익분기 290 × 1.005 → 292, HWM 295 - 5틱 = 290) = 292
        assertThat(pos.getTrailingStopPrice()).isEqualByComparingTo("292");
    }

    @Test
    void signalGatedHoldsWhileLatestScoreIsPositive() {
        props.getBot().setTimeExitMode(TradingProperties.Bot.TimeExitMode.SIGNAL_GATED);
        Signal positive = mock(Signal.class);
        when(positive.getTotalScore()).thenReturn(10);
        when(signals.findLatestByMarket(MARKET)).thenReturn(Optional.of(positive));

        assertThat(tick(aged(), 295.0)).isNull();
    }

    @Test
    void signalGatedClosesOnceLatestScoreTurnsNonPositive() {
        props.getBot().setTimeExitMode(TradingProperties.Bot.TimeExitMode.SIGNAL_GATED);
        Signal negative = mock(Signal.class);
        when(negative.getTotalScore()).thenReturn(-5);
        when(signals.findLatestByMarket(MARKET)).thenReturn(Optional.of(negative));

        assertThat(tick(aged(), 295.0)).isEqualTo(CloseReason.TIME_EXIT);
    }

    @Test
    void losingStalePositionIsNotTimeExitedByDefault() {
        // 290: -0.276%
        assertThat(tick(aged(), 290.0)).isNull();
    }

    @Test
    void lossThresholdTimeExitsSmallLosersOnly() {
        props.getBot().setTimeExitLossThreshold(-0.005);

        assertThat(tick(aged(), 290.0)).isEqualTo(CloseReason.TIME_EXIT);   // -0.276% ≥ -0.5%
        assertThat(tick(aged(), 289.0)).isNull();                            // -0.62% < -0.5% → 손절에 맡김
    }
}
