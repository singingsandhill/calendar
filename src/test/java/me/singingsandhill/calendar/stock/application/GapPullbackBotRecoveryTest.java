package me.singingsandhill.calendar.stock.application;

import me.singingsandhill.calendar.stock.application.observability.StockBotMetrics;
import me.singingsandhill.calendar.stock.application.service.GapPullbackBotService;
import me.singingsandhill.calendar.stock.application.service.PullbackDetectionService;
import me.singingsandhill.calendar.stock.application.service.ScreeningService;
import me.singingsandhill.calendar.stock.application.service.StockMailService;
import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.application.service.StockRiskService;
import me.singingsandhill.calendar.stock.application.service.UniverseBuilder;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 재시작 복구 — 보호 전용 재개 (2026-07-24 리뷰 §6 / P1-5).
 *
 * running 이 인메모리 플래그라 재배포하면 false 가 되어 오픈 포지션의 손절·트레일링·11:20
 * 강제청산까지 전면 중단됐다. 기동 시 오픈 포지션이 있으면 리스크 루프만 자동 재개하고,
 * 신규 진입은 관리자가 명시적으로 start() 할 때까지 차단한다.
 */
class GapPullbackBotRecoveryTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ScreeningService screeningService = mock(ScreeningService.class);
    private final PullbackDetectionService pullbackDetectionService = mock(PullbackDetectionService.class);
    private final StockPositionService positionService = mock(StockPositionService.class);
    private final StockRiskService riskService = mock(StockRiskService.class);
    private final KoreaInvestmentApiClient kisApiClient = mock(KoreaInvestmentApiClient.class);
    private final StockMailService mailService = mock(StockMailService.class);
    private final StockBotMetrics metrics = new StockBotMetrics();
    private final UniverseBuilder universeBuilder = mock(UniverseBuilder.class);

    /** 거래창(09:20~11:20) 한가운데로 고정 — 시간 가드에 걸리지 않게. */
    private static final Clock TRADING_HOURS = Clock.fixed(
        LocalDateTime.of(2026, 7, 24, 10, 0).atZone(KST).toInstant(), KST);

    private GapPullbackBotService botWith(StockProperties props, Clock clock) {
        return new GapPullbackBotService(screeningService, pullbackDetectionService, positionService,
            riskService, kisApiClient, props, mailService, metrics, universeBuilder, clock);
    }

    private StockProperties enabledProps() {
        StockProperties props = new StockProperties();
        props.getBot().setEnabled(true);
        return props;
    }

    @Test
    void autoResume_enablesProtectionWhenOpenPositionsExist() {
        when(positionService.countOpenPositions(any())).thenReturn(2);
        GapPullbackBotService bot = botWith(enabledProps(), TRADING_HOURS);

        bot.resumeProtectionOnStartup();

        GapPullbackBotService.BotStatus status = bot.getStatus();
        assertThat(status.running()).isTrue();
        assertThat(status.recoveryMode()).isTrue();
    }

    @Test
    void autoResume_staysStoppedWhenNoOpenPositions() {
        when(positionService.countOpenPositions(any())).thenReturn(0);
        GapPullbackBotService bot = botWith(enabledProps(), TRADING_HOURS);

        bot.resumeProtectionOnStartup();

        assertThat(bot.getStatus().running()).isFalse();
    }

    @Test
    void autoResume_staysStoppedWhenBotDisabled() {
        when(positionService.countOpenPositions(any())).thenReturn(3);
        GapPullbackBotService bot = botWith(new StockProperties(), TRADING_HOURS); // enabled=false

        bot.resumeProtectionOnStartup();

        assertThat(bot.getStatus().running()).isFalse();
    }

    @Test
    void recoveryMode_runsRiskChecksButSkipsNewEntries() {
        when(positionService.countOpenPositions(any())).thenReturn(1);
        Stock ready = new Stock("005930", "삼성전자", LocalDate.of(2026, 7, 24));
        when(pullbackDetectionService.getEntryReadyStocks(any())).thenReturn(List.of(ready));
        GapPullbackBotService bot = botWith(enabledProps(), TRADING_HOURS);
        bot.resumeProtectionOnStartup();

        bot.executeTradingLoop();

        verify(riskService).checkAndExecuteRiskRules(any());
        verify(pullbackDetectionService).updateAllStockStates(any());
        verify(positionService, never()).openPosition(any());
    }

    @Test
    void explicitStart_clearsRecoveryModeAndAllowsEntries() {
        when(positionService.countOpenPositions(any())).thenReturn(1);
        when(kisApiClient.isConfigured()).thenReturn(true);
        when(pullbackDetectionService.getEntryReadyStocks(any())).thenReturn(List.of());
        GapPullbackBotService bot = botWith(enabledProps(), TRADING_HOURS);
        bot.resumeProtectionOnStartup();

        // 관리자가 명시적으로 start() → 완전 모드
        bot.stop();
        assertThat(bot.start()).isTrue();

        assertThat(bot.getStatus().recoveryMode()).isFalse();
    }
}
