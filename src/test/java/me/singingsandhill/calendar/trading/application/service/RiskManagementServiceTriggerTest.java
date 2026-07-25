package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
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
 * P1-5: 손절/익절/트레일링 트리거(`checkPositionRisk`) 경계 테스트.
 *
 * 자본 보호 핵심 경로인데 테스트가 TimeExit 뿐이었다 (운영 감사 2026-07-06).
 * 판정은 전부 수수료 차감 net PnL% 기준 — entry 1,000 × 10개, 진입수수료 25(0.25%) 고정.
 */
class RiskManagementServiceTriggerTest {

    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final BithumbApiClient bithumbApiClient = mock(BithumbApiClient.class);
    private final TradingEventService tradingEventService = mock(TradingEventService.class);
    private final TradingCircuitBreaker circuitBreaker = mock(TradingCircuitBreaker.class);

    private final RiskManagementService service = new RiskManagementService(
        positionRepository, tradeRepository, bithumbApiClient, props(),
        tradingEventService, circuitBreaker, mock(PlatformTransactionManager.class));

    private static TradingProperties props() {
        TradingProperties props = new TradingProperties();
        props.getRisk().setStopLoss(-0.015);
        props.getRisk().setTakeProfit(0.03);
        props.getRisk().setTrailingStop(0.008);
        props.getRisk().setTrailingActivation(0.015);
        props.getRisk().setTakerFeeRate(0.0025);
        return props;
    }

    /** entry 1,000 / 10개 / 진입수수료 25. SL·TP 가격 필드는 미사용(판정은 net PnL%). */
    private Position position(BigDecimal highWaterMark, boolean trailingActive) {
        return new Position(1L, "KRW-ADA", PositionStatus.OPEN,
            new BigDecimal("1000"), BigDecimal.TEN, new BigDecimal("10000"),
            null, null, null, null, null, null, null,
            null, highWaterMark, trailingActive, null,
            LocalDateTime.now(), null, LocalDateTime.now(),
            new BigDecimal("25"), null, null);
    }

    private CloseReason tick(Position pos, double currentPrice) {
        when(positionRepository.findByMarketAndStatus("KRW-ADA", PositionStatus.OPEN))
            .thenReturn(List.of(pos));
        when(bithumbApiClient.getCurrentPrice()).thenReturn(currentPrice);
        return service.checkAndExecuteRiskRules("KRW-ADA");
    }

    @Test
    void stopLoss_triggersAtNetMinus1_5Percent() {
        // 990: (9,900 - 10,000 - 진입25 - 청산25) / 10,000 = -1.5% → 경계값에서 발동
        CloseReason reason = tick(position(new BigDecimal("1000"), false), 990.0);

        assertThat(reason).isEqualTo(CloseReason.STOP_LOSS);
        verify(bithumbApiClient).placeMarketSellOrder(BigDecimal.TEN);
    }

    @Test
    void smallLoss_aboveStopLossThreshold_holds() {
        // 991: net -1.4% → 손절 미발동, 다른 트리거도 없음
        CloseReason reason = tick(position(new BigDecimal("1000"), false), 991.0);

        assertThat(reason).isNull();
        verify(bithumbApiClient, never()).placeMarketSellOrder(any());
    }

    @Test
    void takeProfit_triggersAtNetPlus3Percent() {
        // 1,036: (10,360 - 10,000 - 25 - 26) / 10,000 = +3.09% ≥ +3%
        CloseReason reason = tick(position(new BigDecimal("1000"), false), 1036.0);

        assertThat(reason).isEqualTo(CloseReason.TAKE_PROFIT);
        verify(bithumbApiClient).placeMarketSellOrder(BigDecimal.TEN);
    }

    @Test
    void trailingActivation_atNetPlus1_5_setsStopWithoutClosing() {
        // 1,021: net +1.59% ≥ +1.5% → 활성화만. 스탑 = 1,021 × (1-0.8%) = 1,012 (내림)
        Position pos = position(new BigDecimal("1000"), false);

        CloseReason reason = tick(pos, 1021.0);

        assertThat(reason).isNull();
        assertThat(pos.isTrailingStopActive()).isTrue();
        assertThat(pos.getTrailingStopPrice()).isEqualByComparingTo("1012");
        verify(bithumbApiClient, never()).placeMarketSellOrder(any());
        verify(positionRepository).save(pos); // 트레일링 상태 영속화
    }

    @Test
    void trailingStop_firesWhenPriceFallsBelowTrailFromHighWaterMark() {
        // HWM 1,050 → 스탑 1,050 × 0.992 = 1,041 (내림). 현재가 1,030 ≤ 1,041 → 발동
        Position pos = position(new BigDecimal("1050"), true);

        CloseReason reason = tick(pos, 1030.0);

        assertThat(reason).isEqualTo(CloseReason.TRAILING_STOP);
        verify(bithumbApiClient).placeMarketSellOrder(BigDecimal.TEN);
    }

    @Test
    void trailingStop_isFlooredAtBreakEven() {
        // HWM 1,010 → 원 스탑 1,001 < 손익분기 1,000 × 1.005 = 1,005 → floor 1,005.
        // 현재가 1,004 ≤ 1,005 → 본전 방어 청산 (floor 없으면 1,001 까지 홀드했을 구간)
        Position pos = position(new BigDecimal("1010"), true);

        CloseReason reason = tick(pos, 1004.0);

        assertThat(reason).isEqualTo(CloseReason.TRAILING_STOP);
        assertThat(pos.getTrailingStopPrice()).isEqualByComparingTo("1005");
    }
}
