package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.EntrySource;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.signal.SignalRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 리밸런싱 포지션의 출구 (ADR trading/risk/0007, 4-C).
 *
 * 6/5 포지션화 이후 리밸런싱 진입(자산 30~70%)이 장중 -1.5% 손절에 걸려 손절 루프가 됐다(10/11~10/20 17건 -4,407원).
 * 엔진이 켜져 있을 때 REBALANCE 포지션은 장중 SL/TP/트레일링/시간청산 대상이 아니고 재난 대비 비상 손절만 받는다.
 * 엔진이 꺼져 있으면 그 포지션을 관리할 주체가 없으므로 일반 출구를 그대로 받는다.
 */
class RiskManagementServiceRebalanceExitTest {

    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final BithumbApiClient api = mock(BithumbApiClient.class);

    private RiskManagementService service(boolean rebalancingEnabled) {
        TradingProperties props = new TradingProperties();
        props.getRisk().setStopLoss(-0.015);
        props.getRisk().setTakeProfit(0.03);
        props.getRisk().setTakerFeeRate(0.0025);
        props.getRebalancing().setEnabled(rebalancingEnabled);
        props.getRebalancing().setCatastrophicStopPct(-0.15);
        return new RiskManagementService(positionRepository, mock(TradeRepository.class), api, props,
            mock(TradingEventService.class), mock(TradingCircuitBreaker.class), mock(PlatformTransactionManager.class),
            mock(IndicatorService.class), mock(SignalRepository.class));
    }

    private Position position(EntrySource source) {
        Position p = new Position(1L, "KRW-ADA", PositionStatus.OPEN,
            new BigDecimal("1000"), BigDecimal.TEN, new BigDecimal("10000"),
            null, null, null, null, null, null, null,
            null, new BigDecimal("1000"), false, null,
            LocalDateTime.now().minusHours(10), null, LocalDateTime.now(),
            new BigDecimal("25"), null, null);
        p.setEntrySource(source);
        return p;
    }

    private CloseReason tick(RiskManagementService service, Position pos, double price) {
        when(positionRepository.findByMarketAndStatus("KRW-ADA", PositionStatus.OPEN)).thenReturn(List.of(pos));
        when(api.getCurrentPrice()).thenReturn(price);
        return service.checkAndExecuteRiskRules("KRW-ADA");
    }

    @Test
    void rebalancePositionIgnoresIntradayStopWhileEngineIsOn() {
        Position pos = position(EntrySource.REBALANCE);

        CloseReason reason = tick(service(true), pos, 970.0);   // net -3.5% — 일반 손절(-1.5%) 구간

        assertThat(reason).isNull();
        verify(api, never()).placeMarketSellOrder(any());
        assertThat(pos.getLowWaterMark()).isEqualByComparingTo("970");   // 관측은 계속
        verify(positionRepository).save(pos);
    }

    @Test
    void rebalancePositionIgnoresTakeProfitAndTimeExitWhileEngineIsOn() {
        CloseReason reason = tick(service(true), position(EntrySource.REBALANCE), 1100.0);   // +9.5%, 보유 10시간

        assertThat(reason).isNull();
        verify(api, never()).placeMarketSellOrder(any());
    }

    @Test
    void rebalancePositionGetsCatastrophicStop() {
        // 840: (8,400 - 10,000 - 25 - 21) / 10,000 = -16.46% ≤ -15%
        CloseReason reason = tick(service(true), position(EntrySource.REBALANCE), 840.0);

        assertThat(reason).isEqualTo(CloseReason.CATASTROPHIC_STOP);
        verify(api).placeMarketSellOrder(BigDecimal.TEN);
    }

    @Test
    void rebalancePositionFallsBackToNormalExitsWhenEngineIsOff() {
        CloseReason reason = tick(service(false), position(EntrySource.REBALANCE), 980.0);   // net -2.5%

        assertThat(reason).isEqualTo(CloseReason.STOP_LOSS);
    }

    @Test
    void signalPositionKeepsNormalStopWhileEngineIsOn() {
        CloseReason reason = tick(service(true), position(EntrySource.SIGNAL), 980.0);

        assertThat(reason).isEqualTo(CloseReason.STOP_LOSS);
    }
}
