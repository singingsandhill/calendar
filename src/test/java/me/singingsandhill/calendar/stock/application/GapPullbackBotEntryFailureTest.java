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
import me.singingsandhill.calendar.stock.domain.stock.EntryOutcome;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 진입 실패 처리 — 재시도 간격·미확인 주문 대기·사유 기록 (ADR stock/infrastructure/0009).
 *
 * 이전에는 실패 종목이 ENTRY_READY 에 남아 5초 틱마다 시세·잔고·매수가능 API 3회를 반복했다.
 */
class GapPullbackBotEntryFailureTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 21, 10, 0);
    private static final Clock CLOCK = Clock.fixed(NOW.atZone(KST).toInstant(), KST);
    private static final LocalDate TODAY = NOW.toLocalDate();

    private final PullbackDetectionService pullbackDetectionService = mock(PullbackDetectionService.class);
    private final StockPositionService positionService = mock(StockPositionService.class);
    private final KoreaInvestmentApiClient kisApiClient = mock(KoreaInvestmentApiClient.class);

    private GapPullbackBotService bot;

    @BeforeEach
    void setUp() {
        StockProperties props = new StockProperties();
        props.getBot().setEnabled(true);
        bot = new GapPullbackBotService(mock(ScreeningService.class), pullbackDetectionService, positionService,
            mock(StockRiskService.class), kisApiClient, props, mock(StockMailService.class), new StockBotMetrics(),
            mock(UniverseBuilder.class), CLOCK, mock(PaperCashLedger.class));
        when(kisApiClient.isConfigured()).thenReturn(true);
        when(positionService.countOpenPositions(any())).thenReturn(0);
        when(positionService.unconfirmedBuyStockCodes()).thenReturn(Set.of());
        bot.start();
    }

    private Stock entryReady(String code) {
        Stock stock = new Stock(code, code, TODAY);
        stock.setOpenPrice(new BigDecimal("9800"));
        stock.setCurrentPrice(new BigDecimal("10000"));
        stock.recordHighFormed(new BigDecimal("10100"));
        stock.recordPullbackStart(new BigDecimal("9980"));
        stock.markEntryReady();
        return stock;
    }

    @Test
    void failedOpenIsRecordedWithOutcomeAndClockTime() {
        Stock stock = entryReady("005930");
        when(pullbackDetectionService.getEntryReadyStocks(TODAY)).thenReturn(List.of(stock));
        when(positionService.openPosition(stock))
            .thenReturn(StockPositionService.OpenResult.failed(EntryOutcome.SIZE_ZERO));

        bot.executeTradingLoop();

        verify(positionService).recordEntryFailure(stock, EntryOutcome.SIZE_ZERO, NOW);
    }

    @Test
    void stockInsideRetryIntervalIsNotRetried() {
        Stock stock = entryReady("005930");
        stock.recordEntryFailure(EntryOutcome.SIZE_ZERO, NOW.minusSeconds(10), 3);   // 기본 간격 30초
        when(pullbackDetectionService.getEntryReadyStocks(TODAY)).thenReturn(List.of(stock));

        bot.executeTradingLoop();

        verify(positionService, never()).openPosition(any());
    }

    @Test
    void stockPastRetryIntervalIsRetried() {
        Stock stock = entryReady("005930");
        stock.recordEntryFailure(EntryOutcome.SIZE_ZERO, NOW.minusSeconds(30), 3);
        when(pullbackDetectionService.getEntryReadyStocks(TODAY)).thenReturn(List.of(stock));
        when(positionService.openPosition(stock))
            .thenReturn(StockPositionService.OpenResult.failed(EntryOutcome.SIZE_ZERO));

        bot.executeTradingLoop();

        verify(positionService).openPosition(stock);
    }

    @Test
    void stockWithUnconfirmedBuyWaitsForSweep() {
        // 접수 여부를 모르는 주문이 남아 있으면 스윕이 판정할 때까지 같은 종목에 재주문하지 않는다.
        Stock stock = entryReady("005930");
        when(pullbackDetectionService.getEntryReadyStocks(TODAY)).thenReturn(List.of(stock));
        when(positionService.unconfirmedBuyStockCodes()).thenReturn(Set.of("005930"));

        bot.executeTradingLoop();

        verify(positionService, never()).openPosition(any());
    }

    @Test
    void openedResultIsNotRecordedAsFailure() {
        Stock stock = entryReady("005930");
        when(pullbackDetectionService.getEntryReadyStocks(TODAY)).thenReturn(List.of(stock));
        when(positionService.openPosition(stock)).thenReturn(StockPositionService.OpenResult.opened(
            me.singingsandhill.calendar.stock.domain.position.StockPosition.open("005930", TODAY,
                new BigDecimal("10000"), 10, new BigDecimal("9880"), new BigDecimal("10100"))));

        bot.executeTradingLoop();

        verify(positionService, never()).recordEntryFailure(any(), any(), any());
    }
}
