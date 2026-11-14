package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.trade.TradeMode;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeStatus;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 재시작 후 자동 시작 + 서킷브레이커 연속손실 복원 (ADR stock/modes/0005).
 *
 * <p>10/20 `.env` 변경 재시작 뒤 두 봇 모두 오픈 포지션이 없어 멈춘 채였다(관리자 start 필요). 그리고 재시작은 인메모리 연속손실
 * 카운터를 0 으로 되돌려, 손실 3연속으로 막혀 있던 신규 진입이 재시작 한 번에 풀렸다. 기동 시 당일 청산 이력(현재 모드)으로
 * 카운터를 다시 세고, {@code trading.bot.auto-start}(기본 false)면 오픈 포지션이 없을 때 완전 시작한다 — 있으면 보호 복구가 이긴다.
 */
class TradingBotAutoStartTest {

    private static final String MARKET = "KRW-ADA";
    private static final LocalDate TODAY = LocalDate.now();

    private final PositionRepository positionRepository = mock(PositionRepository.class);
    private final TradeRepository tradeRepository = mock(TradeRepository.class);
    private final TradingEventService events = mock(TradingEventService.class);
    private final CandleService candleService = mock(CandleService.class);
    private final TradingProperties properties = new TradingProperties();   // 기본 모드 PAPER
    private final TradingCircuitBreaker breaker = new TradingCircuitBreaker(properties);

    @BeforeEach
    void setUp() {
        properties.getBot().setEnabled(true);
        properties.getBot().setMarket(MARKET);
        when(tradeRepository.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of());
    }

    private TradingBotService bot() {
        return new TradingBotService(candleService, mock(SignalService.class), mock(IndicatorService.class),
                mock(RiskManagementService.class), mock(RebalanceService.class), mock(BithumbApiClient.class), tradeRepository,
                positionRepository, properties, events, breaker, mock(AccountSnapshotRepository.class),
                mock(PlatformTransactionManager.class), mock(DailyRegimeService.class));
    }

    private static Position closed(int hour, String pnl, TradeMode mode) {
        LocalDateTime at = TODAY.atTime(hour, 0);
        Position p = new Position(null, MARKET, PositionStatus.CLOSED, new BigDecimal("300"), BigDecimal.ONE,
                new BigDecimal("300"), null, null, null, new BigDecimal(pnl), null, null, null, null, null, false, null,
                at.minusHours(1), at, at.minusHours(1), BigDecimal.ZERO, null, null);
        p.setMode(mode);
        return p;
    }

    private void closedToday(Position... positions) {
        when(positionRepository.findByMarketAndStatusAndClosedAtBetween(eq(MARKET), eq(PositionStatus.CLOSED), any(), any()))
                .thenReturn(List.of(positions));
    }

    @Test
    void autoStartStartsFullyWhenNoOpenPositions() {
        properties.getBot().setAutoStart(true);
        when(positionRepository.countByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(0L);
        closedToday();
        TradingBotService bot = bot();

        bot.resumeProtectionOnStartup();

        assertThat(bot.getStatus().running()).isTrue();
        assertThat(bot.getStatus().recoveryMode()).isFalse();
        verify(events).record(eq(TradingEventLevel.NOTICE), eq("BOT_AUTO_STARTED"), eq(MARKET), any());
    }

    @Test
    void openPositionsKeepProtectionOnlyRecoveryEvenWithAutoStart() {
        properties.getBot().setAutoStart(true);
        when(positionRepository.countByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(1L);
        closedToday();
        TradingBotService bot = bot();

        bot.resumeProtectionOnStartup();

        assertThat(bot.getStatus().running()).isTrue();
        assertThat(bot.getStatus().recoveryMode()).isTrue();
    }

    @Test
    void autoStartIsOffByDefault() {
        assertThat(new TradingProperties().getBot().isAutoStart()).isFalse();
    }

    @Test
    void restoresTodaysTrailingLossStreakForTheCurrentMode() {
        when(positionRepository.countByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(0L);
        // 청산 순: 손실 → 이익 → 손실 → 손실 (+ LIVE 손실 1건은 PAPER 운영 카운터가 아니다). 저장 순서는 뒤섞여 있다
        closedToday(closed(12, "-5", TradeMode.PAPER), closed(9, "-5", TradeMode.PAPER), closed(13, "-5", TradeMode.LIVE),
                closed(10, "7", TradeMode.PAPER), closed(11, "-5", TradeMode.PAPER));

        bot().resumeProtectionOnStartup();

        assertThat(breaker.getConsecutiveLosses()).isEqualTo(2);
    }

    @Test
    void recoveryEventReportsTheRestoredStreakInsteadOfAReset() {
        when(positionRepository.countByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(1L);
        closedToday(closed(9, "-5", TradeMode.PAPER), closed(10, "-5", TradeMode.PAPER));

        bot().resumeProtectionOnStartup();

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(events).record(eq(TradingEventLevel.WARNING), eq("RECOVERY_RESUMED"), eq(MARKET), message.capture());
        assertThat(message.getValue()).contains("연속손실 2회 복원").doesNotContain("리셋");
    }
}
