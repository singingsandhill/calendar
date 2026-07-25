package me.singingsandhill.calendar.stock.application;

import me.singingsandhill.calendar.stock.application.concurrency.StockCodeLocks;
import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.domain.trade.StockTrade;
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
import java.time.LocalDate;
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
        return new StockPositionService(positionRepository, tradeRepository, stockRepository,
            signalRepository, kisApiClient, props, new StockCodeLocks());
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
            new BigDecimal("300000000000"), BigDecimal.ONE,
            BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("120"));
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
            .openPosition(entryReadyStock());

        // 주문 전 시세(100000)가 아니라 실체결 평균가(100500)가 진입가여야 한다
        assertThat(position).isNotNull();
        assertThat(position.getEntryPrice()).isEqualByComparingTo("100500");
        assertThat(position.getEntryQuantity()).isEqualTo(10);
    }

    @Test
    void openPosition_inSimulatedModeSkipsBrokerFillLookup() {
        // PAPER/BACKTEST 는 주문이 시뮬레이션(SIM-...)이라 브로커 원장에 존재하지 않는다.
        // 조회를 시도하면 매 진입마다 "체결 확인 불가" WARN 만 남는다 — 요청가가 곧 체결가.
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("SIM-1"));

        StockPosition position = service.openPosition(entryReadyStock());

        verify(kisApiClient, never()).getTodayOrders();
        assertThat(position).isNotNull();
        assertThat(position.getEntryPrice()).isEqualByComparingTo("100000");
    }

    @Test
    void openPosition_recordsBuyFeeInsteadOfZero() {
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt()))
            .thenReturn(KisOrderResponse.simulated("ODNO-1"));
        when(kisApiClient.getTodayOrders()).thenReturn(orderHistoryWith("ODNO-1", 10, "100000"));

        serviceInMode(StockProperties.Bot.Mode.LIVE).openPosition(entryReadyStock());

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
            BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
            new BigDecimal("120"), "00", "Y", "00", "N");
        when(kisApiClient.getQuote("005930")).thenReturn(halted);

        StockPosition position = service.openPosition(entryReadyStock());

        assertThat(position).isNull();
        verify(kisApiClient, never()).buyMarket(anyString(), anyInt());
    }

    @Test
    void openPosition_persistsPendingTradeBeforeOrderIsSent() {
        stubFundingFor(new BigDecimal("100000"));
        when(kisApiClient.buyMarket(eq("005930"), anyInt())).thenReturn(null); // 응답 유실

        StockPosition position = service.openPosition(entryReadyStock());

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

        StockPosition position = service.openPosition(entryReadyStock());

        // 풀백저가 99800 × 0.99 = 98802 (진입가 대비 -1.198%), 캡(-2% → 98000)보다 타이트
        assertThat(position.getStopLossPrice()).isEqualByComparingTo("98802");
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

        service.executePartialExit(p, 40, new BigDecimal("105000"), StockCloseReason.TP1);

        ArgumentCaptor<StockTrade> captor = ArgumentCaptor.forClass(StockTrade.class);
        verify(tradeRepository, atLeastOnce()).save(captor.capture());
        // 105,000 × 40 × (0.00015 + 0.0020) = 9,030
        assertThat(captor.getValue().getFee()).isEqualByComparingTo("9030");
    }
}
