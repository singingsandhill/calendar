package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.signal.SignalType;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeStatus;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 재시작 복구 — 보호 전용 자동 재개 (ADR trading/modes/0003, stock ADR stock/modes/0003 미러).
 *
 * <p>running 이 인메모리 플래그라 재배포하면 false 가 되어, 관리자가 start() 를 누르기 전까지
 * OPEN 포지션의 손절·익절·트레일링 감시가 전면 중단됐다 (docs/trading/remaining-work.md P1-3).
 * 기동 시 오픈 포지션이 있으면 리스크 루프·스윕만 자동 재개하고, 신규 매매(강신호·리밸런싱·
 * 일반 신호)는 관리자가 명시적으로 start() 할 때까지 차단한다.
 */
class TradingBotRecoveryTest {

    private static final String MARKET = "KRW-ADA";

    private final CandleService candleService = mock(CandleService.class);
    private final SignalService signalService = mock(SignalService.class);
    private final IndicatorService indicatorService = mock(IndicatorService.class);
    private final RiskManagementService riskManagementService = mock(RiskManagementService.class);
    private final RebalanceService rebalanceService = mock(RebalanceService.class);
    private final BithumbApiClient bithumbApiClient = mock(BithumbApiClient.class);
    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final TradingEventService tradingEventService = mock(TradingEventService.class);
    private final TradingCircuitBreaker circuitBreaker = mock(TradingCircuitBreaker.class);
    private final AccountSnapshotRepository accountSnapshotRepository = mock(AccountSnapshotRepository.class);

    @BeforeEach
    void stubSweepEmpty() {
        // 기동 스윕(§8-G)·틱 스윕이 미결 주문을 찾지 않도록 비운다.
        when(tradeRepository.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of());
    }

    private TradingBotService botWith(boolean enabled) {
        TradingProperties properties = new TradingProperties();
        properties.getBot().setEnabled(enabled);
        properties.getBot().setMarket(MARKET);
        return new TradingBotService(candleService, signalService, indicatorService,
                riskManagementService, rebalanceService, bithumbApiClient, tradeRepository,
                positionRepository, properties, tradingEventService, circuitBreaker,
                accountSnapshotRepository, mock(PlatformTransactionManager.class));
    }

    @Test
    void autoResume_enablesProtectionWhenOpenPositionsExist() {
        when(positionRepository.countByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(2L);
        TradingBotService bot = botWith(true);

        bot.resumeProtectionOnStartup();

        TradingBotService.BotStatus status = bot.getStatus();
        assertThat(status.running()).isTrue();
        assertThat(status.recoveryMode()).isTrue();
        // 자동 재개는 조용히 일어나면 안 된다 — 서킷브레이커 카운터 리셋까지 포함해 이벤트로 남긴다.
        verify(tradingEventService).record(eq(TradingEventLevel.WARNING), eq("RECOVERY_RESUMED"),
                eq(MARKET), any());
    }

    @Test
    void autoResume_staysStoppedWhenNoOpenPositions() {
        when(positionRepository.countByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(0L);
        TradingBotService bot = botWith(true);

        bot.resumeProtectionOnStartup();

        assertThat(bot.getStatus().running()).isFalse();
    }

    /**
     * 강한 BUY 신호의 happy-path 를 전부 스텁해 게이트가 없으면 실제로 주문 전송까지 도달하게
     * 만든다 — 얕은 스텁이면 executeBuy 가 잔고 null 등에서 조기 return 해 게이트 없이도
     * never() 가 성립하는 공허한 테스트가 된다.
     */
    private void stubStrongBuyHappyPath() {
        when(riskManagementService.checkAndExecuteRiskRules(MARKET)).thenReturn(null);
        Signal strongBuy = mock(Signal.class);
        when(strongBuy.getTotalScore()).thenReturn(80);
        when(strongBuy.getSignalType()).thenReturn(SignalType.BUY);
        when(signalService.generateSignal(MARKET)).thenReturn(strongBuy);
        when(circuitBreaker.isEntryBlocked(any(), any())).thenReturn(false);
        when(bithumbApiClient.getKrwBalance()).thenReturn(
                new BithumbAccountResponse("KRW", "1000000", "0", null, null, "KRW"));
        when(bithumbApiClient.getCurrentPrice()).thenReturn(1000.0);
        when(positionRepository.countByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(1L);
        when(positionRepository.findByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(List.of());
    }

    @Test
    void recoveryMode_runsRiskChecksButSkipsNewTrades() {
        stubStrongBuyHappyPath();
        TradingBotService bot = botWith(true);
        bot.resumeProtectionOnStartup();

        bot.executeTradeLoop();

        // 보호(리스크 체크)와 관측(신호 기록)은 돌고, 매매 단계(강신호·리밸런싱)는 차단된다.
        verify(riskManagementService).checkAndExecuteRiskRules(MARKET);
        verify(signalService).generateSignal(MARKET);
        verify(bithumbApiClient, never()).placeMarketBuyOrder(any());
        verify(bithumbApiClient, never()).placeMarketBuyOrder(any(), any());
        verify(rebalanceService, never()).checkAndExecute(any());
    }

    @Test
    void autoResume_staysStoppedWhenBotDisabled() {
        when(positionRepository.countByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(3L);
        TradingBotService bot = botWith(false);

        bot.resumeProtectionOnStartup();

        assertThat(bot.getStatus().running()).isFalse();
    }

    /**
     * 자동 재개로 running 이 이미 true 인 상태에서 관리자 Start 한 번으로 완전 재개돼야 한다.
     * start() 가 CAS(false→true)뿐이면 이 경로에서 false 를 반환하고 recoveryMode 가 영원히
     * 남는다 — stop→start 2단계를 강요하면 그 사이 리스크 보호가 끊긴다.
     */
    @Test
    void explicitStart_withoutStop_clearsRecoveryMode() {
        when(positionRepository.countByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(1L);
        TradingBotService bot = botWith(true);
        bot.resumeProtectionOnStartup();

        assertThat(bot.start()).isTrue();

        TradingBotService.BotStatus status = bot.getStatus();
        assertThat(status.running()).isTrue();
        assertThat(status.recoveryMode()).isFalse();
    }

    /** stop→start 2단계 경로에서도 recovery 가 잔존하면 안 된다 (stock 미러 케이스). */
    @Test
    void stopThenStart_alsoClearsRecoveryMode() {
        when(positionRepository.countByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(1L);
        TradingBotService bot = botWith(true);
        bot.resumeProtectionOnStartup();

        bot.stop();
        assertThat(bot.start()).isTrue();

        assertThat(bot.getStatus().recoveryMode()).isFalse();
    }

    /** 게이트 해제의 대조 실험 — 4번과 동일 스텁에서 start 후에는 주문이 실제로 나간다. */
    @Test
    void afterStartCleared_strongSignalPlacesOrder() {
        stubStrongBuyHappyPath();
        TradingBotService bot = botWith(true);
        bot.resumeProtectionOnStartup();
        bot.start();

        bot.executeTradeLoop();

        verify(bithumbApiClient).placeMarketBuyOrder(any());
    }
}
