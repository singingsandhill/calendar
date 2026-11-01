package me.singingsandhill.calendar.stock.application;

import me.singingsandhill.calendar.stock.application.observability.StockBotMetrics;
import me.singingsandhill.calendar.stock.application.service.GapPullbackBotService;
import me.singingsandhill.calendar.stock.application.service.PaperCashLedger;
import me.singingsandhill.calendar.stock.application.service.PullbackDetectionService;
import me.singingsandhill.calendar.stock.application.service.ScreeningService;
import me.singingsandhill.calendar.stock.application.service.StockMailService;
import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.application.service.StockRiskService;
import me.singingsandhill.calendar.stock.application.service.UniverseBuilder;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 봇 상태에 운영 모드와 PAPER 가용 예수금을 노출 (ADR stock/modes/0004).
 *
 * 0체결 51거래일 동안 상태 API 로는 "PAPER 인데 쓸 돈이 0원" 을 볼 방법이 없었다.
 */
class GapPullbackBotPaperStatusTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(
        LocalDateTime.of(2026, 10, 21, 10, 0).atZone(KST).toInstant(), KST);

    private final PaperCashLedger ledger = mock(PaperCashLedger.class);

    private GapPullbackBotService botIn(StockProperties.Bot.Mode mode) {
        StockProperties props = new StockProperties();
        props.getBot().setMode(mode);
        return new GapPullbackBotService(mock(ScreeningService.class), mock(PullbackDetectionService.class),
            mock(StockPositionService.class), mock(StockRiskService.class), mock(KoreaInvestmentApiClient.class),
            props, mock(StockMailService.class), new StockBotMetrics(), mock(UniverseBuilder.class), CLOCK,
            ledger);
    }

    @Test
    void paperStatusShowsModeAndLedgerCash() {
        when(ledger.availableToday()).thenReturn(new BigDecimal("8997849.70"));

        GapPullbackBotService.BotStatus status = botIn(StockProperties.Bot.Mode.PAPER).getStatus();

        assertThat(status.mode()).isEqualTo("PAPER");
        assertThat(status.paperAvailableCash()).isEqualByComparingTo("8997849.70");
    }

    @Test
    void liveStatusHasNoPaperCash() {
        GapPullbackBotService.BotStatus status = botIn(StockProperties.Bot.Mode.LIVE).getStatus();

        assertThat(status.mode()).isEqualTo("LIVE");
        assertThat(status.paperAvailableCash()).isNull();
        verify(ledger, never()).availableToday();
    }
}
