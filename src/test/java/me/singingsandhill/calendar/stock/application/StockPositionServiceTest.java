package me.singingsandhill.calendar.stock.application;

import me.singingsandhill.calendar.stock.application.concurrency.StockCodeLocks;
import me.singingsandhill.calendar.stock.application.service.PaperCashLedger;
import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.domain.position.StockPositionStatus;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.stock.EntryOutcome;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockProductType;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.domain.stock.StockState;
import me.singingsandhill.calendar.stock.domain.trade.StockTrade;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeMode;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeRepository;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeStatus;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisOrderDetailResponse;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisOrderResponse;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisQuoteResponse;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 포지션 청산 주문 안전 가드 회귀 테스트.
 *
 * 사고 이력 (2026-07-24 리뷰 §3-⑤): 매도 주문이 도메인 수량 검증보다 먼저 나가서,
 * 잔여수량 초과 요청 시 LIVE 에서는 거부 주문 스팸(동일 종목 별도 보유분이 있으면 오매도),
 * PAPER 에서는 매 틱 IllegalArgumentException 이 발생했다.
 * 기대: 주문 발행 전에 수량이 검증되어 초과 요청은 주문 자체가 나가지 않는다.
 */
class StockPositionServiceTest {

    private final StockPositionRepository positionRepository = mock(StockPositionRepository.class);
    private final StockTradeRepository tradeRepository = mock(StockTradeRepository.class);
    private final StockRepository stockRepository = mock(StockRepository.class);
    private final StockSignalRepository signalRepository = mock(StockSignalRepository.class);
    private final KoreaInvestmentApiClient kisApiClient = mock(KoreaInvestmentApiClient.class);

    /** 기본 서비스 — 기본 모드 PAPER (주문 시뮬레이션). */
    private final StockPositionService service = serviceInMode(StockProperties.Bot.Mode.PAPER);

    private StockPositionService serviceInMode(StockProperties.Bot.Mode mode) {
        StockProperties props = new StockProperties();
        props.getBot().setMode(mode);
        return serviceWith(props);
    }

    private StockPositionService serviceWith(StockProperties props) {
        // 원장은 실물(PaperCashLedger) — 거래 저장소 mock 이 비어 있으면 paper-cash 전액이 가용액이다.
        return new StockPositionService(positionRepository, tradeRepository, stockRepository,
            signalRepository, kisApiClient, props, new StockCodeLocks(),
            new PaperCashLedger(tradeRepository, props, Clock.systemDefaultZone()));
    }

    private StockPosition positionWithRemaining40() {
        StockPosition p = StockPosition.open(
            "005930", LocalDate.of(2026, 7, 24),
            new BigDecimal("100000"), 100,
            new BigDecimal("95000"), new BigDecimal("103000"));
        p.executePartialExit(60, new BigDecimal("103000"), StockCloseReason.TP2,
            new BigDecimal("0.00015"), new BigDecimal("0.0015"));
        return p;
    }

    // ===== 진입: 실체결가 backfill · 선영속화 · 동적 손절 (2026-07-24 리뷰 P1-1/P1-4) =====

    private Stock entryReadyStock() {
        Stock stock = new Stock("005930", "삼성전자", LocalDate.of(2026, 7, 24));
        stock.setId(7L);
        stock.setOpenPrice(new BigDecimal("98000"));
        stock.setCurrentPrice(new BigDecimal("100000"));
        stock.recordHighFormed(new BigDecimal("101000"));
        stock.recordPullbackStart(new BigDecimal("99800"));
        stock.markEntryReady();
        return stock;
    }

    private void stubFundingFor(BigDecimal price) {
        when(kisApiClient.getQuote("005930")).thenReturn(quoteAt(price));
        when(kisApiClient.getAvailableCash()).thenReturn(new BigDecimal("10000000"));
        when(kisApiClient.getBuyableQuantity(eq("005930"), any())).thenReturn(100);
        when(positionRepository.save(any(StockPosition.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(tradeRepository.save(any(StockTrade.class)))
            .thenAnswer(inv -> inv.getArgument(0));
    }

    private static KisQuoteResponse quoteAt(BigDecimal price) {
        return new KisQuoteResponse("005930", price, new BigDecimal("98000"),
            new BigDecimal("101000"), new BigDecimal("97000"), new BigDecimal("97500"),
            BigDecimal.ZERO, BigDecimal.ZERO, 1_000_000L, new BigDecimal("1000000000"),
            new BigDecimal("300000000000"), BigDecimal.ONE);
    }

    private static KisOrderDetailResponse orderHistoryWith(String odno, int filledQty, String avgPrice) {
        return new KisOrderDetailResponse("0", "0", "ok", List.of(
            new KisOrderDetailResponse.OrderDetail(odno, "005930", "삼성전자", "02",
                filledQty, new BigDecimal("0"), filledQty, new BigDecimal(avgPrice),
                "092500", "01", null, "시장가", "체결")));
    }

    @Test
    void openPosition_usesActualFillPriceFromOrderHistory() {
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("ODNO-1"));
        when(kisApiClient.getTodayOrders()).thenReturn(orderHistoryWith("ODNO-1", 10, "100500"));

        StockPosition position = serviceInMode(StockProperties.Bot.Mode.LIVE)
            .openPosition(entryReadyStock()).position();

        // 주문 전 시세(100000)가 아니라 실체결 평균가(100500)가 진입가여야 한다
        assertThat(position).isNotNull();
        assertThat(position.getEntryPrice()).isEqualByComparingTo("100500");
        assertThat(position.getEntryQuantity()).isEqualTo(10);
    }

    @Test
    void openPosition_inSimulatedModeSkipsBrokerFillLookup() {
        // PAPER/BACKTEST 는 주문이 시뮬레이션(SIM-...)이라 브로커 원장에 존재하지 않는다.
        // 조회를 시도하면 매 진입마다 "체결 확인 불가" WARN 만 남는다 — 체결가는 시뮬레이션이 정한다
        // (요청가 + 슬리피지 0.2%, 호가단위 올림 — ADR stock/modes/0004).
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("SIM-1"));

        StockPosition position = service.openPosition(entryReadyStock()).position();

        verify(kisApiClient, never()).getTodayOrders();
        assertThat(position).isNotNull();
        assertThat(position.getEntryPrice()).isEqualByComparingTo("100200");
    }

    // ===== PAPER 가상 예수금 원장 + 체결 현실화 (ADR stock/modes/0004) =====

    private static StockTrade filledSimBuy(int qty, String price, String fee) {
        StockTrade t = StockTrade.createBuyOrder("SIM-0", "000660", qty, new BigDecimal(price), true);
        t.markFilled(new BigDecimal(price), qty, new BigDecimal(fee));
        return t;
    }

    @Test
    void openPosition_inPaperMode_sizesFromLedgerWithoutTouchingRealAccount() {
        // 51거래일 0체결 사고: PAPER 사이징이 실계좌 예수금(0원)을 읽어 수량 0 이었다.
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.getAvailableCash()).thenReturn(BigDecimal.ZERO);   // 실계좌는 비어 있다
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("SIM-1"));
        // 당일 다른 종목에 5,000,000 + 750 을 이미 썼다 → 가용 4,999,250 → 10% = 499,925 → 4주
        when(tradeRepository.findByOrderedAtBetween(any(), any()))
            .thenReturn(List.of(filledSimBuy(50, "100000", "750")));

        StockPosition position = service.openPosition(entryReadyStock()).position();

        assertThat(position).isNotNull();
        assertThat(position.getEntryQuantity()).isEqualTo(4);
        verify(kisApiClient, never()).getAvailableCash();
        verify(kisApiClient, never()).getBuyableQuantity(anyString(), any());
    }

    @Test
    void openPosition_inPaperMode_capsQuantityToLedgerCash() {
        // getBuyableQuantity 의 PAPER 대응 — 체결가(슬리피지 포함)·매수 수수료로 원장 가용액을 넘지 않는다.
        StockProperties props = new StockProperties();
        props.getBot().setPaperCash(new BigDecimal("1000000"));
        props.getRisk().setPositionSizeRatio(BigDecimal.ONE);
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("SIM-1"));

        StockPosition position = serviceWith(props).openPosition(entryReadyStock()).position();

        // 목표 10주(1,000,000 / 100,000) 이지만 체결가 100,200 × (1 + 0.015%) 로는 9주까지만 산다
        assertThat(position.getEntryQuantity()).isEqualTo(9);
    }

    @Test
    void openPosition_inLiveMode_stillSizesFromBrokerAccount() {
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("ODNO-1"));
        when(kisApiClient.getTodayOrders()).thenReturn(orderHistoryWith("ODNO-1", 10, "100000"));

        serviceInMode(StockProperties.Bot.Mode.LIVE).openPosition(entryReadyStock()).position();

        verify(kisApiClient).getAvailableCash();
        verify(kisApiClient).getBuyableQuantity(eq("005930"), any());
        verify(tradeRepository, never()).findByOrderedAtBetween(any(), any());
    }

    @Test
    void openPosition_inPaperMode_fillsAtSlippedPriceRoundedUpToTick() {
        // 6,730 × 1.002 = 6,743.46 → 5,000~20,000 구간 호가단위 10원 올림 = 6,750
        stubFundingFor(new BigDecimal("6730"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("SIM-1"));

        StockPosition position = service.openPosition(entryReadyStock()).position();

        assertThat(position.getEntryPrice()).isEqualByComparingTo("6750");
        ArgumentCaptor<StockTrade> captor = ArgumentCaptor.forClass(StockTrade.class);
        verify(tradeRepository, atLeastOnce()).save(captor.capture());
        StockTrade filled = captor.getAllValues().stream()
            .filter(t -> t.getExecutedPrice() != null).findFirst().orElseThrow();
        assertThat(filled.getExecutedPrice()).isEqualByComparingTo("6750");
    }

    @Test
    void openPosition_inPaperMode_usesTheEtfTickTable() {
        // ADR stock/algorithm/0011 — ETF 는 2,000원 이상 5원: 6,743.46 → 6,745 (주식 표면 6,750)
        stubFundingFor(new BigDecimal("6730"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("SIM-1"));
        Stock etf = entryReadyStock();
        etf.setProductType(StockProductType.ETF);

        StockPosition position = service.openPosition(etf).position();

        assertThat(position.getEntryPrice()).isEqualByComparingTo("6745");
    }

    @Test
    void partialExit_inPaperMode_usesTheStocksProductType() {
        // 10,300 × 0.998 = 10,279.4 → ETF 5원 내림 10,275 (주식 표 10원이면 10,270)
        Stock etf = new Stock("069500", "KODEX 200", LocalDate.of(2026, 7, 24));
        etf.setId(9L);
        etf.setProductType(StockProductType.ETF);
        when(stockRepository.findById(9L)).thenReturn(Optional.of(etf));
        StockPosition p = StockPosition.open("069500", LocalDate.of(2026, 7, 24),
            new BigDecimal("10000"), 100, new BigDecimal("9800"), new BigDecimal("10300"));
        p.setStockId(9L);
        when(kisApiClient.sellMarket(eq("069500"), eq(60)))
            .thenReturn(KisOrderResponse.simulated("SIM-S"));
        when(tradeRepository.save(any(StockTrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.save(any(StockPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        service.executePartialExit(p, 60, new BigDecimal("10300"), StockCloseReason.TP2);

        ArgumentCaptor<StockTrade> captor = ArgumentCaptor.forClass(StockTrade.class);
        verify(tradeRepository).save(captor.capture());
        assertThat(captor.getValue().getExecutedPrice()).isEqualByComparingTo("10275");
    }

    @Test
    void partialExit_inPaperMode_fillsAtSlippedPriceRoundedDownToTick() {
        // 103,000 × 0.998 = 102,794 → 50,000~200,000 구간 호가단위 100원 내림 = 102,700
        StockPosition p = StockPosition.open("005930", LocalDate.of(2026, 7, 24),
            new BigDecimal("100000"), 100, new BigDecimal("98000"), new BigDecimal("103000"));
        when(kisApiClient.sellMarket(eq("005930"), eq(60)))
            .thenReturn(KisOrderResponse.simulated("SIM-S"));
        when(tradeRepository.save(any(StockTrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.save(any(StockPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        service.executePartialExit(p, 60, new BigDecimal("103000"), StockCloseReason.TP2);

        ArgumentCaptor<StockTrade> captor = ArgumentCaptor.forClass(StockTrade.class);
        verify(tradeRepository).save(captor.capture());
        assertThat(captor.getValue().getExecutedPrice()).isEqualByComparingTo("102700");
        // 102,700 × 60 × (0.00015 + 0.0020) = 13,248.3
        assertThat(captor.getValue().getFee()).isEqualByComparingTo("13248.3");
        assertThat(p.getAverageExitPrice()).isEqualByComparingTo("102700");
    }

    @Test
    void partialExit_inLiveMode_recordsRequestedPrice() {
        StockPosition p = StockPosition.open("005930", LocalDate.of(2026, 7, 24),
            new BigDecimal("100000"), 100, new BigDecimal("98000"), new BigDecimal("103000"));
        when(kisApiClient.sellMarket(eq("005930"), eq(60)))
            .thenReturn(KisOrderResponse.simulated("ODNO-S"));
        when(tradeRepository.save(any(StockTrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.save(any(StockPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        serviceInMode(StockProperties.Bot.Mode.LIVE)
            .executePartialExit(p, 60, new BigDecimal("103000"), StockCloseReason.TP2);

        ArgumentCaptor<StockTrade> captor = ArgumentCaptor.forClass(StockTrade.class);
        verify(tradeRepository).save(captor.capture());
        assertThat(captor.getValue().getExecutedPrice()).isEqualByComparingTo("103000");
    }

    @Test
    void openPosition_recordsBuyFeeInsteadOfZero() {
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("ODNO-1"));
        when(kisApiClient.getTodayOrders()).thenReturn(orderHistoryWith("ODNO-1", 10, "100000"));

        serviceInMode(StockProperties.Bot.Mode.LIVE).openPosition(entryReadyStock()).position();

        ArgumentCaptor<StockTrade> captor = ArgumentCaptor.forClass(StockTrade.class);
        verify(tradeRepository, atLeastOnce()).save(captor.capture());
        StockTrade filled = captor.getAllValues().stream()
            .filter(t -> t.getFee() != null).findFirst().orElseThrow();
        // 1,000,000 × 0.00015 = 150
        assertThat(filled.getFee()).isEqualByComparingTo("150");
    }

    @Test
    void openPosition_refusesWhenStockIsHaltedAtEntryTime() {
        // 스크리닝(09:20) 통과 후에도 진입 시점에 VI/거래정지가 걸릴 수 있다 (리뷰 §6 / P2-3).
        // 자금·주문은 정상 스텁 — 가드가 없으면 매수가 나가는 조건에서 검증한다.
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("ODNO-1"));
        when(kisApiClient.getTodayOrders()).thenReturn(orderHistoryWith("ODNO-1", 10, "100000"));

        KisQuoteResponse halted = new KisQuoteResponse("005930", new BigDecimal("100000"),
            new BigDecimal("98000"), new BigDecimal("101000"), new BigDecimal("97000"),
            new BigDecimal("97500"), BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.ONE,
            BigDecimal.ONE, BigDecimal.ONE, "00", "Y", "00", "N");
        when(kisApiClient.getQuote("005930")).thenReturn(halted);

        StockPosition position = service.openPosition(entryReadyStock()).position();

        assertThat(position).isNull();
        verify(kisApiClient, never()).buyMarket(anyString(), anyInt());
    }

    @Test
    void openPosition_persistsPendingTradeBeforeOrderIsSent() {
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt())).thenReturn(null); // 응답 유실

        StockPosition position = service.openPosition(entryReadyStock()).position();

        // 주문 접수 여부 불명 — 포지션은 만들지 않되 흔적(PENDING 거래)은 남아야 스윕이 수습한다
        assertThat(position).isNull();
        ArgumentCaptor<StockTrade> captor = ArgumentCaptor.forClass(StockTrade.class);
        verify(tradeRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues())
            .anyMatch(t -> t.getStatus() == StockTradeStatus.PENDING && t.isBuy());
    }

    @Test
    void openPosition_stopLossAnchoredOnPullbackLow() {
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("ODNO-1"));
        when(kisApiClient.getTodayOrders()).thenReturn(orderHistoryWith("ODNO-1", 10, "100000"));

        StockPosition position = service.openPosition(entryReadyStock()).position();

        // 풀백저가 99800 × 0.99 = 98802 (진입가 대비 -1.198%), 캡(-2% → 98000)보다 타이트
        assertThat(position.getStopLossPrice()).isEqualByComparingTo("98802");
    }

    // ===== 진입 결과 사유 (ADR stock/infrastructure/0009) =====

    @Test
    void openPosition_reportsQuoteUnavailable() {
        when(kisApiClient.getQuote("005930")).thenReturn(null);

        StockPositionService.OpenResult result = service.openPosition(entryReadyStock());

        assertThat(result.outcome()).isEqualTo(EntryOutcome.QUOTE_UNAVAILABLE);
        assertThat(result.position()).isNull();
    }

    @Test
    void openPosition_reportsNotTradable() {
        KisQuoteResponse halted = new KisQuoteResponse("005930", new BigDecimal("100000"),
            new BigDecimal("98000"), new BigDecimal("101000"), new BigDecimal("97000"),
            new BigDecimal("97500"), BigDecimal.ZERO, BigDecimal.ZERO, 1L, BigDecimal.ONE,
            BigDecimal.ONE, BigDecimal.ONE, "00", "Y", "00", "N");
        when(kisApiClient.getQuote("005930")).thenReturn(halted);

        assertThat(service.openPosition(entryReadyStock()).outcome()).isEqualTo(EntryOutcome.NOT_TRADABLE);
    }

    @Test
    void openPosition_reportsSizeZero() {
        // 51거래일 0체결의 실제 사유 — 이제는 로그가 아니라 결과로 돌아와 Stock 에 남는다.
        StockProperties props = new StockProperties();
        props.getBot().setPaperCash(BigDecimal.ZERO);
        stubFundingFor(new BigDecimal("100000"));

        StockPositionService.OpenResult result = serviceWith(props).openPosition(entryReadyStock());

        assertThat(result.outcome()).isEqualTo(EntryOutcome.SIZE_ZERO);
        verify(kisApiClient, never()).buyMarket(anyString(), anyInt());
    }

    @Test
    void openPosition_reportsOrderUnconfirmed() {
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt())).thenReturn(null);

        assertThat(service.openPosition(entryReadyStock()).outcome()).isEqualTo(EntryOutcome.ORDER_UNCONFIRMED);
    }

    @Test
    void openPosition_reportsErrorInsteadOfThrowing() {
        // 예외가 트랜잭션 프록시를 넘으면 그 틱 전체(다른 종목의 청산 기록 포함)가 롤백 전용이 된다.
        when(kisApiClient.getQuote("005930")).thenThrow(new IllegalStateException("KIS down"));

        StockPositionService.OpenResult result = service.openPosition(entryReadyStock());

        assertThat(result.outcome()).isEqualTo(EntryOutcome.ERROR);
    }

    @Test
    void openPosition_reportsOpenedWithPosition() {
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("SIM-1"));

        StockPositionService.OpenResult result = service.openPosition(entryReadyStock());

        assertThat(result.outcome()).isEqualTo(EntryOutcome.OPENED);
        assertThat(result.position()).isNotNull();
    }

    @Test
    void recordEntryFailure_persistsReasonAndMovesToEntryFailedAtMax() {
        StockProperties props = new StockProperties();
        props.getEntry().setMaxOpenAttempts(2);
        Stock stock = entryReadyStock();
        LocalDateTime now = LocalDateTime.of(2026, 7, 24, 9, 40);

        serviceWith(props).recordEntryFailure(stock, EntryOutcome.SIZE_ZERO, now);
        serviceWith(props).recordEntryFailure(stock, EntryOutcome.SIZE_ZERO, now.plusSeconds(30));

        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        Stock saved = captor.getValue();
        assertThat(saved.getEntryFailCount()).isEqualTo(2);
        assertThat(saved.getLastEntryFailReason()).isEqualTo(EntryOutcome.SIZE_ZERO);
        assertThat(saved.getState()).isEqualTo(StockState.ENTRY_FAILED);
    }

    @Test
    void unconfirmedBuyStockCodes_listsOnlyPendingBrokerUnknownBuys() {
        StockTrade pending = StockTrade.createBuyOrder(StockTrade.PENDING_ORDER_ID_PREFIX + "1",
            "005930", 10, new BigDecimal("100000"), true);
        StockTrade confirmed = StockTrade.createBuyOrder("0000123", "000660", 10, new BigDecimal("50000"), true);
        confirmed.markFilled(new BigDecimal("50000"), 10, new BigDecimal("75"));
        when(tradeRepository.findTodayTrades()).thenReturn(List.of(pending, confirmed));

        assertThat(service.unconfirmedBuyStockCodes()).containsExactly("005930");
    }

    // ===== 운영 모드 필터 (ADR stock/observability/0003) =====

    private static StockPosition closedIn(StockTradeMode mode, String code) {
        return StockPosition.reconstitute(1L, null, code, LocalDate.of(2026, 10, 21),
            StockPositionStatus.CLOSED, new BigDecimal("10000"), 10, new BigDecimal("100000"),
            LocalDateTime.of(2026, 10, 21, 9, 40), 0, new BigDecimal("10100"),
            false, true, false, null, new BigDecimal("9800"), null, null, false,
            new BigDecimal("700"), new BigDecimal("0.7"), new BigDecimal("101000"), 10,
            StockCloseReason.TP2, LocalDateTime.of(2026, 10, 21, 10, 0),
            LocalDateTime.of(2026, 10, 21, 9, 40), null, mode);
    }

    @Test
    void closedPositions_showOnlyCurrentBotMode() {
        // PAPER 실측(ADR modes/0004)이 쌓이면 대시보드·손익 요약이 두 모드를 합산하게 된다.
        when(positionRepository.findClosedPositions(any())).thenReturn(List.of(
            closedIn(StockTradeMode.PAPER, "111111"), closedIn(StockTradeMode.LIVE, "222222"),
            closedIn(null, "333333")));

        assertThat(service.getClosedPositions(LocalDate.of(2026, 10, 21)))
            .extracting(StockPosition::getStockCode).containsExactly("111111");
    }

    @Test
    void closedPositions_inLiveModeTreatLegacyNullAsLive() {
        when(positionRepository.findClosedPositions(any())).thenReturn(List.of(
            closedIn(StockTradeMode.PAPER, "111111"), closedIn(StockTradeMode.LIVE, "222222"),
            closedIn(null, "333333")));

        assertThat(serviceInMode(StockProperties.Bot.Mode.LIVE).getClosedPositions(LocalDate.of(2026, 10, 21)))
            .extracting(StockPosition::getStockCode).containsExactly("222222", "333333");
    }

    @Test
    void positionsInRangeAndOpenAndAllAreModeFilteredToo() {
        when(positionRepository.findByTradingDateBetween(any(), any())).thenReturn(List.of(
            closedIn(StockTradeMode.PAPER, "111111"), closedIn(StockTradeMode.LIVE, "222222")));
        when(positionRepository.findByTradingDate(any())).thenReturn(List.of(
            closedIn(StockTradeMode.PAPER, "111111"), closedIn(StockTradeMode.LIVE, "222222")));
        when(positionRepository.findOpenPositions(any())).thenReturn(List.of(
            closedIn(StockTradeMode.LIVE, "222222")));
        LocalDate day = LocalDate.of(2026, 10, 21);

        assertThat(service.getPositionsInRange(day, day)).extracting(StockPosition::getStockCode)
            .containsExactly("111111");
        assertThat(service.getAllPositions(day)).extracting(StockPosition::getStockCode)
            .containsExactly("111111");
        assertThat(service.getOpenPositions(day)).isEmpty();
    }

    // ===== 고아 체결 스윕 (2026-07-24 리뷰 §3-④ / P1-1) =====

    private StockTrade unconfirmedPendingBuy() {
        StockTrade t = StockTrade.createBuyOrder(
            StockTrade.PENDING_ORDER_ID_PREFIX + "1", "005930", 10,
            new BigDecimal("100000"), true);
        t.setId(42L);
        return t;
    }

    @Test
    void reconcile_createsPositionForOrphanFill() {
        // 주문은 체결됐는데 응답이 유실돼 포지션이 없는 상태 = 무보호 포지션
        when(tradeRepository.findTodayTrades()).thenReturn(List.of(unconfirmedPendingBuy()));
        when(kisApiClient.getTodayOrders()).thenReturn(orderHistoryWith("ODNO-9", 10, "100200"));
        when(positionRepository.findByStockCodeAndTradingDateAndStatusNot(
            eq("005930"), any(), any())).thenReturn(Optional.empty());
        when(stockRepository.findByStockCodeAndTradingDate(eq("005930"), any()))
            .thenReturn(Optional.of(entryReadyStock()));
        when(positionRepository.save(any(StockPosition.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tradeRepository.save(any(StockTrade.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reconcileUnconfirmedOrders(LocalDate.of(2026, 7, 24));

        ArgumentCaptor<StockPosition> captor = ArgumentCaptor.forClass(StockPosition.class);
        verify(positionRepository).save(captor.capture());
        StockPosition recovered = captor.getValue();
        assertThat(recovered.getEntryPrice()).isEqualByComparingTo("100200");
        assertThat(recovered.getEntryQuantity()).isEqualTo(10);
        assertThat(recovered.getStopLossPrice()).isNotNull();
    }

    @Test
    void reconcile_doesNotDuplicatePositionWhenOneAlreadyExists() {
        when(tradeRepository.findTodayTrades()).thenReturn(List.of(unconfirmedPendingBuy()));
        when(kisApiClient.getTodayOrders()).thenReturn(orderHistoryWith("ODNO-9", 10, "100200"));
        when(positionRepository.findByStockCodeAndTradingDateAndStatusNot(eq("005930"), any(), any()))
            .thenReturn(Optional.of(StockPosition.open("005930", LocalDate.of(2026, 7, 24),
                new BigDecimal("100200"), 10, new BigDecimal("98000"), new BigDecimal("101000"))));
        when(tradeRepository.save(any(StockTrade.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reconcileUnconfirmedOrders(LocalDate.of(2026, 7, 24));

        verify(positionRepository, never()).save(any(StockPosition.class));
    }

    @Test
    void reconcile_givesUpAfterMaxAttemptsAndCancelsTrade() {
        StockTrade pending = unconfirmedPendingBuy();
        when(tradeRepository.findTodayTrades()).thenReturn(List.of(pending));
        // 브로커에 해당 체결이 없음 = 주문 미접수
        when(kisApiClient.getTodayOrders())
            .thenReturn(new KisOrderDetailResponse("0", "0", "ok", List.of()));
        when(tradeRepository.save(any(StockTrade.class))).thenAnswer(inv -> inv.getArgument(0));

        for (int i = 0; i < StockPositionService.MAX_RECONCILE_ATTEMPTS; i++) {
            service.reconcileUnconfirmedOrders(LocalDate.of(2026, 7, 24));
        }

        assertThat(pending.getStatus()).isEqualTo(StockTradeStatus.CANCELLED);
        verify(positionRepository, never()).save(any(StockPosition.class));
    }

    @Test
    void partialExit_exceedingRemaining_doesNotPlaceOrder() {
        StockPosition p = positionWithRemaining40();

        service.executePartialExit(p, 50, new BigDecimal("105000"), StockCloseReason.TP1);

        verify(kisApiClient, never()).sellMarket(anyString(), anyInt());
    }

    @Test
    void partialExit_zeroOrNegativeQuantity_doesNotPlaceOrder() {
        StockPosition p = positionWithRemaining40();

        service.executePartialExit(p, 0, new BigDecimal("105000"), StockCloseReason.TP1);

        verify(kisApiClient, never()).sellMarket(anyString(), anyInt());
    }

    @Test
    void partialExit_recordsSellCostInTradeLedger() {
        // 매도 원장 fee=0 회귀 방지 — 포지션 손익과 동일 비용 모델(수수료+거래세)로 기록돼야 한다
        StockPosition p = positionWithRemaining40();
        when(kisApiClient.sellMarket(eq("005930"), eq(40)))
            .thenReturn(KisOrderResponse.simulated("SIM-S"));
        when(tradeRepository.save(any(StockTrade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(positionRepository.save(any(StockPosition.class))).thenAnswer(inv -> inv.getArgument(0));

        // LIVE: 매도 체결가 = 요청가 (PAPER 슬리피지 체결은 partialExit_inPaperMode_* 가 본다)
        serviceInMode(StockProperties.Bot.Mode.LIVE)
            .executePartialExit(p, 40, new BigDecimal("105000"), StockCloseReason.TP1);

        ArgumentCaptor<StockTrade> captor = ArgumentCaptor.forClass(StockTrade.class);
        verify(tradeRepository, atLeastOnce()).save(captor.capture());
        // 105,000 × 40 × (0.00015 + 0.0020) = 9,030
        assertThat(captor.getValue().getFee()).isEqualByComparingTo("9030");
    }
}
