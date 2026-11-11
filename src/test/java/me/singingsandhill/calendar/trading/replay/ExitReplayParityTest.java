package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.application.service.IndicatorService;
import me.singingsandhill.calendar.trading.application.service.RiskManagementService;
import me.singingsandhill.calendar.trading.application.service.TradingCircuitBreaker;
import me.singingsandhill.calendar.trading.application.service.TradingEventService;
import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.signal.SignalRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties.Bot.TimeExitMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 판정 규칙 복제 드리프트 가드 (ADR trading/observability/0005) — 리플레이의 CLOSE 모델(매분 종가 한 번)은 운영
 * {@code RiskManagementService} 가 매분 현재가로 하는 판정과 같아야 한다. 같은 가격 경로에서 출구 사유와 출구 분이 같은지 본다.
 * 시간청산은 운영이 벽시계를 쓰므로 여기선 끄고(maxHold 0) 손절·트레일링·익절·틱 하한만 맞춘다. 이 테스트가 깨지면 리플레이로
 * 고른 값이 운영에서 다르게 동작한다는 뜻이다.
 */
class ExitReplayParityTest {

    static Stream<Arguments> paths() {
        List<Arguments> cases = new ArrayList<>();
        List<String[]> prices = List.of(
                new String[]{"289", "288", "287", "286", "285", "284", "283"},
                new String[]{"293", "296", "298", "297", "296", "295", "294", "293", "292"},
                new String[]{"292", "295", "299", "301"},
                new String[]{"291", "290", "289", "288", "290", "296", "297", "293", "291", "290"},
                new String[]{"295", "297", "302", "300", "299", "296", "294"});
        for (String[] path : prices) {
            for (int ticks : new int[]{0, 5}) {
                for (String entryFee : new String[]{"0", "1.16"}) {
                    cases.add(Arguments.of(String.join(",", path), ticks, entryFee));
                }
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0} ticks={1} entryFee={2}")
    @MethodSource("paths")
    void closeModelMatchesTheProductionRiskLoop(String path, int ticks, String entryFee) {
        String[] prices = path.split(",");

        int productionIndex = -1;
        CloseReason productionReason = null;
        TradingProperties props = props(ticks);
        PositionRepository repository = mock(PositionRepository.class);
        BithumbApiClient api = mock(BithumbApiClient.class);
        RiskManagementService service = new RiskManagementService(repository, mock(TradeRepository.class), api, props,
                mock(TradingEventService.class), mock(TradingCircuitBreaker.class), mock(PlatformTransactionManager.class),
                mock(IndicatorService.class), mock(SignalRepository.class));
        Position live = new Position(1L, "KRW-ADA", PositionStatus.OPEN, new BigDecimal("290"), BigDecimal.TEN,
                new BigDecimal("2900"), null, null, null, null, null, null, null, null, new BigDecimal("290"), false, null,
                LocalDateTime.now(), null, LocalDateTime.now(), new BigDecimal(entryFee), null, null);
        when(repository.findByMarketAndStatus("KRW-ADA", PositionStatus.OPEN)).thenReturn(List.of(live));
        for (int i = 0; i < prices.length; i++) {
            when(api.getCurrentPrice()).thenReturn(Double.valueOf(prices[i]));
            CloseReason reason = service.checkAndExecuteRiskRules("KRW-ADA");
            if (reason != null) {
                productionIndex = i;
                productionReason = reason;
                break;
            }
        }

        LocalDateTime opened = LocalDateTime.of(2026, 8, 1, 10, 0, 30);
        List<ReplayBar> bars = new ArrayList<>(List.of(ReplayBar.flat(opened.withSecond(0), new BigDecimal("290"))));
        for (int i = 0; i < prices.length; i++) {
            bars.add(ReplayBar.flat(opened.withSecond(0).plusMinutes(i + 1), new BigDecimal(prices[i])));
        }
        ReplayPosition replayed = new ReplayPosition(1L, opened, opened.plusDays(1), new BigDecimal("290"), BigDecimal.TEN,
                new BigDecimal(entryFee), new BigDecimal("290"), "STOP_LOSS", BigDecimal.ZERO);
        ExitReplayConfig config = new ExitReplayConfig(-0.015, 0.03, 0.008, 0.015, ticks, ticks,
                TimeExitMode.BREAKEVEN_CLOSE, 0, 0.0025, 0.0004, PriceModel.CLOSE);
        ExitOutcome outcome = ExitReplayEngine.simulate(replayed, new MinuteSeries(bars, List.of()), config);

        if (productionReason == null) {
            assertThat(outcome.reason()).isEqualTo("END_OF_DATA");
        } else {
            assertThat(outcome.reason()).isEqualTo(productionReason.name());
            assertThat(outcome.exitTime()).isEqualTo(opened.withSecond(0).plusMinutes(productionIndex + 1));
        }
    }

    private static TradingProperties props(int ticks) {
        TradingProperties props = new TradingProperties();
        props.getRisk().setStopLoss(-0.015);
        props.getRisk().setTakeProfit(0.03);
        props.getRisk().setTrailingStop(0.008);
        props.getRisk().setTrailingActivation(0.015);
        props.getRisk().setTakerFeeRate(0.0025);
        props.getRisk().setMinStopTicks(ticks);
        props.getRisk().setMinTrailingTicks(ticks);
        props.getBot().setMaxHoldMinutes(0);
        return props;
    }
}
