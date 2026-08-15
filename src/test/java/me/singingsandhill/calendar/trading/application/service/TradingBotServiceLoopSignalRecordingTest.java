package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.signal.SignalType;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeStatus;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import me.singingsandhill.calendar.trading.application.service.RebalanceService.RebalanceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 리스크 청산이 발동한 틱에도 신호를 기록한다 (ADR trading/observability/0003).
 *
 * <p>이전 동작: {@code executeTradeLoop} 이 {@code checkAndExecuteRiskRules} 가 CloseReason 을
 * 돌려주면 그 자리에서 return 해 신호 생성 단계까지 가지 않았다. 그래서 손절·익절·트레일링이
 * 발동한 분에는 {@code trading_signals} 행이 통째로 없었다 — 분석에 가장 필요한 순간(청산 직전의
 * 지표 상태)이 1분 시계열에서 정확히 빠져 있었다는 뜻이다.
 *
 * <p>이 클래스가 고정하는 안전 불변식은 두 개다.
 * <ul>
 *   <li>리스크 체크는 <b>여전히 최우선</b>이다 — 신호 생성은 그보다 앞설 수 없다. 앞서면 신호
 *       생성의 예외·지연이 손절을 막는다. {@code riskCheckRunsBeforeSignalGeneration} 이 순서를
 *       {@code InOrder} 로 못박아, 나중에 누가 순서를 뒤집으면 빌드가 깨진다.</li>
 *   <li>청산 후에는 <b>여전히 매매 단계(4~6)를 건너뛴다</b> — 기록만 추가하고 판단은 그대로다.</li>
 * </ul>
 */
class TradingBotServiceLoopSignalRecordingTest {

    private static final String MARKET = "KRW-ADA";

    private final CandleService candleService = mock(CandleService.class);
    private final SignalService signalService = mock(SignalService.class);
    private final RiskManagementService riskManagementService = mock(RiskManagementService.class);
    private final RebalanceService rebalanceService = mock(RebalanceService.class);
    private final BithumbApiClient bithumbApiClient = mock(BithumbApiClient.class);
    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final TradingEventService tradingEventService = mock(TradingEventService.class);

    private TradingBotService service;

    @BeforeEach
    void setUp() {
        TradingProperties properties = new TradingProperties();
        properties.getBot().setMarket(MARKET);

        service = new TradingBotService(
                candleService, signalService, null, riskManagementService, rebalanceService,
                bithumbApiClient, tradeRepository, null, properties, tradingEventService,
                null, null, mock(PlatformTransactionManager.class));

        // 기동 스윕(§8-G)과 틱 스윕이 미결 주문을 찾지 않도록 비운다.
        when(tradeRepository.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of());
        service.start();
    }

    /**
     * 약한 HOLD 신호. 반드시 바깥 {@code when(...)} 호출 <b>밖에서</b> 만들어야 한다 —
     * 스터빙 진행 중에 또 다른 mock 을 스터빙하면 Mockito 가 UnfinishedStubbingException 을 던진다.
     */
    private Signal holdSignal;

    @BeforeEach
    void stubSignal() {
        holdSignal = mock(Signal.class);
        when(holdSignal.getTotalScore()).thenReturn(0);
        when(holdSignal.getSignalType()).thenReturn(SignalType.HOLD);
    }

    @Test
    void riskExitTick_stillGeneratesSignal() {
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(CloseReason.STOP_LOSS);
        when(signalService.generateSignal(MARKET)).thenReturn(holdSignal);

        service.executeTradeLoop();

        // 이전 동작에서는 조기 return 때문에 아예 호출되지 않았다.
        verify(signalService).generateSignal(MARKET);
    }

    @Test
    void riskExitTick_skipsRebalanceAndTrade() {
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(CloseReason.TAKE_PROFIT);
        when(signalService.generateSignal(MARKET)).thenReturn(holdSignal);

        service.executeTradeLoop();

        // 기록만 추가했을 뿐 판단은 그대로여야 한다.
        verify(rebalanceService, never()).checkAndExecute(any());
        verify(bithumbApiClient, never()).placeMarketBuyOrder(any());
    }

    @Test
    void riskCheckRunsBeforeSignalGeneration() {
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(null);
        when(signalService.generateSignal(MARKET)).thenReturn(holdSignal);
        when(rebalanceService.checkAndExecute(MARKET)).thenReturn(new RebalanceResult(false, null, null, null));

        service.executeTradeLoop();

        // 순서가 뒤집히면 신호 생성의 예외·지연이 손절을 막는다. 영구 가드.
        InOrder order = inOrder(riskManagementService, signalService);
        order.verify(riskManagementService).checkAndExecuteRiskRules(MARKET);
        order.verify(signalService).generateSignal(MARKET);
    }

    @Test
    void signalGenerationFailureOnRiskExitTick_doesNotRaiseNewLoopError() {
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(CloseReason.TRAILING_STOP);
        when(signalService.generateSignal(MARKET)).thenThrow(new IllegalStateException("indicator source down"));

        service.executeTradeLoop();

        // 청산은 이미 끝났다. 관측을 위해 추가한 코드가 없던 운영 경보를 만들어내면 안 된다.
        TradingBotService.BotStatus status = service.getStatus();
        assertThat(status.lastError()).isNull();
        verify(tradingEventService, never()).record(any(), eq("LOOP_ERROR"), any(), any());
    }

    @Test
    void signalNullOnRiskExitTick_returnsQuietlyWithoutFabricatingRow() {
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(CloseReason.STOP_LOSS);
        when(signalService.generateSignal(MARKET)).thenReturn(null);

        service.executeTradeLoop();

        // 캔들이 없어 신호를 못 만든 분은 진짜 결측이다. 합성 행을 넣으면 균일 1분 관측
        // 시계열이라는 전제가 깨져 전방수익·구간 통계가 전부 틀어진다.
        verify(rebalanceService, never()).checkAndExecute(any());
        verify(bithumbApiClient, never()).placeMarketBuyOrder(any());
    }
}
