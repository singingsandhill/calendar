package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.signal.SignalType;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeStatus;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import me.singingsandhill.calendar.trading.application.service.RebalanceService.RebalanceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 캔들 수집 실패가 그 틱의 손절·익절을 막지 않는다 (ADR trading/risk/0006).
 *
 * <p>이전 동작: {@code executeTradeLoop} 의 1단계 {@code candleService.fetchAndSaveCandles()} 만
 * 바깥 catch 에 노출돼 있었다. 0단계(미결 주문 스윕)와 3단계(신호 생성)는 각각 try/catch 로
 * 감싸여 있는데 그 사이 한 줄만 무방비였고, 캔들 API 장애나 캔들 동기화 ↔ 트레이딩 루프 동시
 * insert 유니크 위반이 나면 예외가 바깥 catch 로 빠져 2단계 리스크 체크에 <b>도달하지 못했다.</b>
 *
 * <p>이 클래스가 고정하는 것은 비대칭 두 개다.
 * <ul>
 *   <li><b>자본 보호는 살린다</b> — 캔들이 실패해도 리스크 체크는 돈다.
 *       {@code RiskManagementService} 는 캔들을 참조하지 않고 실시간 현재가로 판정하므로
 *       캔들이 낡아도 오발동하지 않는다.</li>
 *   <li><b>신규 리스크는 감수하지 않는다</b> — stale 캔들로 계산한 신호가 4~6단계의 신규 진입·
 *       리밸런싱을 태우면 안 된다. 감싸기만 하고 끝내면 그 회귀가 생긴다.</li>
 * </ul>
 */
class TradingBotServiceCandleFailureGuardTest {

    private static final String MARKET = "KRW-ADA";

    private final CandleService candleService = mock(CandleService.class);
    private final SignalService signalService = mock(SignalService.class);
    private final RiskManagementService riskManagementService = mock(RiskManagementService.class);
    private final RebalanceService rebalanceService = mock(RebalanceService.class);
    private final BithumbApiClient bithumbApiClient = mock(BithumbApiClient.class);
    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final TradingEventService tradingEventService = mock(TradingEventService.class);
    // 정상 틱 가드가 6단계까지 실제로 관통하려면 필요하다 — null 이면 NPE 가 바깥 catch 로 빠져
    // "정상 틱인데 lastError 가 남는" 하네스 아티팩트가 생긴다.
    private final PositionRepository positionRepository = mock(PositionRepository.class);

    private TradingBotService service;

    @BeforeEach
    void setUp() {
        TradingProperties properties = new TradingProperties();
        properties.getBot().setMarket(MARKET);

        service = new TradingBotService(
                candleService, signalService, null, riskManagementService, rebalanceService,
                bithumbApiClient, tradeRepository, positionRepository, properties, tradingEventService,
                null, null, mock(PlatformTransactionManager.class));

        // 기동 스윕(§8-G)과 틱 스윕이 미결 주문을 찾지 않도록 비운다.
        when(tradeRepository.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of());
        service.start();
    }

    /**
     * 약한 HOLD 신호. 바깥 {@code when(...)} 호출 <b>밖에서</b> 만들어야 한다 —
     * 스터빙 진행 중에 또 다른 mock 을 스터빙하면 Mockito 가 UnfinishedStubbingException 을 던진다.
     */
    private Signal holdSignal;

    @BeforeEach
    void stubSignal() {
        holdSignal = mock(Signal.class);
        when(holdSignal.getTotalScore()).thenReturn(0);
        when(holdSignal.getSignalType()).thenReturn(SignalType.HOLD);
    }

    private void candleSyncFails() {
        when(candleService.fetchAndSaveCandles())
                .thenThrow(new IllegalStateException("candle API unavailable"));
    }

    @Test
    void candleSyncFailure_stillRunsRiskCheck() {
        candleSyncFails();
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(null);
        when(signalService.generateSignal(MARKET)).thenReturn(holdSignal);

        service.executeTradeLoop();

        // 이전 동작에서는 예외가 바깥 catch 로 빠져 여기 도달하지 못했다.
        verify(riskManagementService).checkAndExecuteRiskRules(MARKET);
    }

    @Test
    void candleSyncFailure_stillClosesPositionOnStopLoss() {
        candleSyncFails();
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(CloseReason.STOP_LOSS);
        when(signalService.generateSignal(MARKET)).thenReturn(holdSignal);

        service.executeTradeLoop();

        // 이 변경이 지키려는 것 그 자체 — 캔들 장애 중에도 손절이 실행된다.
        verify(riskManagementService).checkAndExecuteRiskRules(MARKET);
    }

    @Test
    void candleSyncFailure_suppressesEntryAndRebalance() {
        candleSyncFails();
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(null);
        when(signalService.generateSignal(MARKET)).thenReturn(holdSignal);

        service.executeTradeLoop();

        // stale 캔들로 계산한 신호가 신규 진입·리밸런싱을 태우면 안 된다.
        // 감싸기만 하고 4~6 을 그대로 두면 이전보다 나쁜 동작이 된다.
        verify(rebalanceService, never()).checkAndExecute(any());
        verify(bithumbApiClient, never()).placeMarketBuyOrder(any());
    }

    @Test
    void candleSyncFailure_keepsOperatorAlert() {
        candleSyncFails();
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(null);
        when(signalService.generateSignal(MARKET)).thenReturn(holdSignal);

        service.executeTradeLoop();

        // 이전에는 바깥 catch 가 lastError + LOOP_ERROR 로 경보를 남겼다. 루프를 계속 돌리면서
        // 그 경보를 잃으면 캔들 장애가 조용히 지속된다. lastError 는 BotStatus 로 대시보드에
        // 노출되므로 그대로 채우고, 이벤트 타입만 실제 동작에 맞게 나눈다.
        TradingBotService.BotStatus status = service.getStatus();
        assertThat(status.lastError()).contains("IllegalStateException");
        verify(tradingEventService).record(
                eq(TradingEventLevel.WARNING), eq("CANDLE_SYNC_FAILED"), eq(MARKET), contains("IllegalStateException"));
        verify(tradingEventService, never()).record(any(), eq("LOOP_ERROR"), any(), any());
    }

    /** 가드 — 변경 전에도 통과해야 한다. 캔들이 정상이면 4~6 단계는 그대로 실행된다. */
    @Test
    void healthyTick_proceedsToRebalanceAndTrade() {
        when(candleService.fetchAndSaveCandles()).thenReturn(1);
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(null);
        when(signalService.generateSignal(MARKET)).thenReturn(holdSignal);
        when(rebalanceService.checkAndExecute(MARKET)).thenReturn(new RebalanceResult(false, null, null, null));

        service.executeTradeLoop();

        verify(rebalanceService).checkAndExecute(MARKET);
        assertThat(service.getStatus().lastError()).isNull();
    }
}
