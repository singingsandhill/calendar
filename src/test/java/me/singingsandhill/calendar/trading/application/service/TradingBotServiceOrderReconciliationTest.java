package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeStatus;
import me.singingsandhill.calendar.trading.domain.trade.TradeType;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * §8-B: 선영속화(Trade SUBMITTED + client_order_id) + 틱 스윕 + Position 생성.
 * v2 생성 응답엔 trades 가 없어 "접수됐는데 체결정보 미확보(UNKNOWN)" 갭이 남는다 — 체결된 매수가
 * Position(SL/TP) 없이 방치되지 않도록 주문 전 선영속화하고, 틱 스윕이 SUBMITTED 를 정합화한다.
 */
class TradingBotServiceOrderReconciliationTest {

    private static final String MARKET = "KRW-ADA";

    private BithumbApiClient api;
    private TradeRepository tradeRepo;
    private PositionRepository posRepo;
    private TradingCircuitBreaker breaker;
    private AccountSnapshotRepository snapRepo;
    private IndicatorService indicators;
    private RiskManagementService risk;
    private RebalanceService rebalance;
    private CandleService candles;
    private SignalService signals;
    private RiskManagementService riskLoop;
    private TradingBotService svc;
    private final List<TradeStatus> statusAtSave = new ArrayList<>();

    @BeforeEach
    void setUp() {
        api = mock(BithumbApiClient.class);
        tradeRepo = mock(TradeRepository.class);
        posRepo = mock(PositionRepository.class);
        breaker = mock(TradingCircuitBreaker.class);
        snapRepo = mock(AccountSnapshotRepository.class);
        indicators = mock(IndicatorService.class);
        risk = mock(RiskManagementService.class);
        rebalance = mock(RebalanceService.class);
        candles = mock(CandleService.class);
        signals = mock(SignalService.class);
        PlatformTransactionManager txm = mock(PlatformTransactionManager.class);

        svc = new TradingBotService(candles, signals, indicators, risk, rebalance, api, tradeRepo, posRepo,
                new TradingProperties(), mock(TradingEventService.class), breaker, snapRepo, txm);

        // 서킷브레이커·잔고·가드 통과 (ExecutedVolumeTest 하네스와 동일)
        when(breaker.isEntryBlocked(any(), any())).thenReturn(false);
        when(snapRepo.findFirstByMarketAndDateRange(any(), any(), any())).thenReturn(Optional.empty());
        when(posRepo.findByMarketAndStatusAndClosedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        when(api.getKrwBalance()).thenReturn(acct("KRW", "1000000"));
        when(api.getCurrentPrice()).thenReturn(1000.0);
        when(posRepo.findByMarketAndStatus(any(), any())).thenReturn(List.of());
        when(api.getCoinBalance()).thenReturn(acct("ADA", "0"));
        when(indicators.calculateATRPercent(any())).thenReturn(null);
        when(risk.calculateStopLossPrice(any())).thenReturn(new BigDecimal("985"));
        when(risk.calculateTakeProfitPrice(any())).thenReturn(new BigDecimal("1030"));
        // §8-B 게이트: cid 가 주문에 부착되는 구성
        when(api.supportsClientOrderId()).thenReturn(true);
        when(api.newClientOrderId()).thenReturn("cid-1");
        // 저장 시점의 status 기록 (같은 인스턴스가 뒤에서 변이되므로 캡처로는 순서 검증 불가)
        when(tradeRepo.save(any())).thenAnswer(inv -> {
            Trade t = inv.getArgument(0);
            statusAtSave.add(t.getStatus());
            return t;
        });
        when(posRepo.save(any())).thenAnswer(inv -> {
            Position p = inv.getArgument(0);
            p.setId(77L);
            return p;
        });
    }

    private BithumbAccountResponse acct(String currency, String balance) {
        return new BithumbAccountResponse(currency, balance, "0", null, null, "KRW");
    }

    /** 1000원 × 248개 체결, 수수료 621.875 (ExecutedVolumeTest 와 동일 픽스처). */
    private BithumbOrderResponse buyFill(String uuid) {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "t1", "1000", "248", "248000", "bid", "2026-07-08T10:00:00");
        return new BithumbOrderResponse(uuid, "bid", "price", "1000", "done", MARKET,
                null, "248", "0", "0", "0", "621.875", "0", "248", 1, List.of(t));
    }

    private BithumbOrderResponse unknownPartial(String uuid) {
        return new BithumbOrderResponse(uuid, "bid", "price", null, "UNKNOWN", MARKET,
                null, null, null, null, null, null, null, null, null, null);
    }

    private BithumbOrderResponse stateOnly(String uuid, String state) {
        return new BithumbOrderResponse(uuid, "bid", "price", null, state, MARKET,
                null, null, null, null, null, null, null, null, null, null);
    }

    private Signal buySignal() {
        Signal signal = mock(Signal.class);
        when(signal.getTotalScore()).thenReturn(50);
        return signal;
    }

    private Trade submitted(String cid, LocalDateTime orderedAt) {
        return new Trade(10L, cid, null, MARKET, TradeType.BUY, "market",
                BigDecimal.ZERO, new BigDecimal("248750"), null, null, null,
                TradeStatus.SUBMITTED, 50, "Auto buy signal", orderedAt, null, orderedAt, cid);
    }

    // ==================== executeBuy: 선영속화 ====================

    @Test
    void executeBuy_prePersistsSubmittedTrade_beforePlacingOrder() {
        when(api.placeMarketBuyOrder(any(), eq("cid-1"))).thenReturn(buyFill("ex-uuid"));

        svc.executeBuy(MARKET, buySignal());

        // 순서: SUBMITTED 저장 → 주문 전송
        InOrder order = inOrder(tradeRepo, api);
        order.verify(tradeRepo).save(any(Trade.class));
        order.verify(api).placeMarketBuyOrder(any(), eq("cid-1"));
        assertThat(statusAtSave.get(0)).isEqualTo(TradeStatus.SUBMITTED);

        ArgumentCaptor<Trade> saved = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
        assertThat(saved.getValue().getClientOrderId()).isEqualTo("cid-1");
    }

    @Test
    void executeBuy_confirmedFill_updatesSameTradeToDone_withExchangeUuid_andOpensPosition() {
        when(api.placeMarketBuyOrder(any(), eq("cid-1"))).thenReturn(buyFill("ex-uuid"));

        svc.executeBuy(MARKET, buySignal());

        // 같은 Trade 가 SUBMITTED → DONE 으로 갱신 저장됨
        assertThat(statusAtSave).containsExactly(TradeStatus.SUBMITTED, TradeStatus.DONE);
        ArgumentCaptor<Trade> saved = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo, org.mockito.Mockito.times(2)).save(saved.capture());
        Trade trade = saved.getValue();
        assertThat(trade.getUuid()).isEqualTo("ex-uuid");          // 거래소 uuid 로 갱신
        assertThat(trade.getClientOrderId()).isEqualTo("cid-1");
        assertThat(trade.getExecutedVolume()).isEqualByComparingTo("248");
        assertThat(trade.getPositionId()).isEqualTo(77L);          // Position 연결

        ArgumentCaptor<Position> pos = ArgumentCaptor.forClass(Position.class);
        verify(posRepo).save(pos.capture());
        assertThat(pos.getValue().getEntryVolume()).isEqualByComparingTo("248");
        assertThat(pos.getValue().getStopLossPrice()).isEqualByComparingTo("985");
        assertThat(pos.getValue().getTakeProfitPrice()).isEqualByComparingTo("1030");
    }

    @Test
    void executeBuy_nullResponse_leavesSubmitted_noPosition() {
        when(api.placeMarketBuyOrder(any(), eq("cid-1"))).thenReturn(null);

        svc.executeBuy(MARKET, buySignal());

        // 선영속화 1회만 — FAILED 로 덮지 않고 SUBMITTED 유지(스윕이 수습)
        assertThat(statusAtSave).containsExactly(TradeStatus.SUBMITTED);
        verify(posRepo, never()).save(any());
    }

    @Test
    void executeBuy_unknownStateResponse_leavesSubmitted_noPosition_noPriceFallback() {
        when(api.placeMarketBuyOrder(any(), eq("cid-1"))).thenReturn(unknownPartial("oid-9"));

        svc.executeBuy(MARKET, buySignal());

        // UNKNOWN = 접수됐으나 체결정보 미확보 — 현재가 폴백으로 DONE 처리하지 않는다
        assertThat(statusAtSave).containsExactly(TradeStatus.SUBMITTED);
        verify(posRepo, never()).save(any());
        verify(api, never()).getOrder(anyString());
    }

    @Test
    void executeBuy_gateOff_noPrePersist_behavesAsBefore() {
        when(api.supportsClientOrderId()).thenReturn(false);
        when(api.placeMarketBuyOrder(any())).thenReturn(buyFill("ex-uuid"));

        svc.executeBuy(MARKET, buySignal());

        // 선영속화 없음 — 기존 동작(체결 후 DONE 저장 1회)
        assertThat(statusAtSave).containsExactly(TradeStatus.DONE);
        verify(api, never()).placeMarketBuyOrder(any(), anyString());
        verify(posRepo).save(any());
    }

    @Test
    void executeBuy_blockedWhenUnresolvedSubmittedExists() {
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED))
                .thenReturn(List.of(submitted("cid-old", LocalDateTime.now().minusSeconds(30))));

        svc.executeBuy(MARKET, buySignal());

        // 미해결 SUBMITTED 존재 → 신규 매수 금지 (중복 진입 차단)
        verify(api, never()).placeMarketBuyOrder(any());
        verify(api, never()).placeMarketBuyOrder(any(), anyString());
        assertThat(statusAtSave).isEmpty();
    }

    // ==================== 틱 스윕: reconcileSubmittedOrders ====================

    @Test
    void sweep_confirmedFill_marksDone_andOpensPositionWithSlTp() {
        Trade trade = submitted("cid-7", LocalDateTime.now().minusSeconds(30));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));
        BithumbOrderResponse.TradeDetail fill = new BithumbOrderResponse.TradeDetail(
                MARKET, "t1", "1000", "5", "5000", "bid", "2026-07-08T10:00:00");
        when(api.getOrderByClientOrderId("cid-7")).thenReturn(new BithumbOrderResponse(
                "ex-7", "bid", "price", "1000", "done", MARKET, null,
                "5", "0", "0", "0", "12.5", "0", "5", 1, List.of(fill)));

        svc.reconcileSubmittedOrders(MARKET);

        assertThat(trade.getStatus()).isEqualTo(TradeStatus.DONE);
        assertThat(trade.getUuid()).isEqualTo("ex-7");
        assertThat(trade.getExecutedPrice()).isEqualByComparingTo("1000");
        assertThat(trade.getExecutedVolume()).isEqualByComparingTo("5");
        assertThat(trade.getFee()).isEqualByComparingTo("12.5");
        assertThat(trade.getPositionId()).isEqualTo(77L);

        ArgumentCaptor<Position> pos = ArgumentCaptor.forClass(Position.class);
        verify(posRepo).save(pos.capture());
        assertThat(pos.getValue().getEntryPrice()).isEqualByComparingTo("1000");
        assertThat(pos.getValue().getEntryVolume()).isEqualByComparingTo("5");
        assertThat(pos.getValue().getStopLossPrice()).isEqualByComparingTo("985");
        assertThat(pos.getValue().getTakeProfitPrice()).isEqualByComparingTo("1030");
        assertThat(pos.getValue().getEntryFee()).isEqualByComparingTo("12.5");
    }

    @Test
    void sweep_cancelledOrder_marksCancel_noPosition() {
        Trade trade = submitted("cid-8", LocalDateTime.now().minusSeconds(30));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));
        when(api.getOrderByClientOrderId("cid-8")).thenReturn(stateOnly("ex-8", "cancel"));

        svc.reconcileSubmittedOrders(MARKET);

        assertThat(trade.getStatus()).isEqualTo(TradeStatus.CANCEL);
        verify(posRepo, never()).save(any());
    }

    @Test
    void sweep_notFoundAfterExpiry_marksFailed() {
        Trade trade = submitted("cid-9", LocalDateTime.now().minusMinutes(3));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));
        when(api.getOrderByClientOrderId("cid-9")).thenReturn(null);

        svc.reconcileSubmittedOrders(MARKET);

        // 만료(2분) 초과 + 미발견 = 거래소 미도달로 간주
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.FAILED);
        verify(tradeRepo).save(trade);
        verify(posRepo, never()).save(any());
    }

    @Test
    void sweep_notFoundWithinExpiry_keepsSubmitted() {
        Trade trade = submitted("cid-10", LocalDateTime.now().minusSeconds(30));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));
        when(api.getOrderByClientOrderId("cid-10")).thenReturn(null);

        svc.reconcileSubmittedOrders(MARKET);

        assertThat(trade.getStatus()).isEqualTo(TradeStatus.SUBMITTED);
        verify(tradeRepo, never()).save(any());
    }

    @Test
    void sweep_waitState_keepsSubmitted() {
        Trade trade = submitted("cid-11", LocalDateTime.now().minusSeconds(30));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));
        when(api.getOrderByClientOrderId("cid-11")).thenReturn(stateOnly("ex-11", "wait"));

        svc.reconcileSubmittedOrders(MARKET);

        assertThat(trade.getStatus()).isEqualTo(TradeStatus.SUBMITTED);
        verify(tradeRepo, never()).save(any());
    }

    @Test
    void sweep_withinGracePeriod_doesNotQueryExchange() {
        // 방금 전송된 주문(in-flight)은 건드리지 않는다
        Trade trade = submitted("cid-12", LocalDateTime.now().minusSeconds(3));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));

        svc.reconcileSubmittedOrders(MARKET);

        verify(api, never()).getOrderByClientOrderId(anyString());
    }

    @Test
    void sweep_otherMarket_ignored() {
        Trade trade = new Trade(11L, "cid-13", null, "KRW-BTC", TradeType.BUY, "market",
                BigDecimal.ZERO, new BigDecimal("10000"), null, null, null,
                TradeStatus.SUBMITTED, null, null, LocalDateTime.now().minusMinutes(1), null,
                LocalDateTime.now().minusMinutes(1), "cid-13");
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));

        svc.reconcileSubmittedOrders(MARKET);

        verify(api, never()).getOrderByClientOrderId(anyString());
    }

    // ==================== executeSell: 매도 선영속화 (§8-B 확장) ====================

    /** 1100원 × 10개 매도 체결, 수수료 27.5 (PersistenceTest 와 동일 픽스처). */
    private BithumbOrderResponse sellFill(String uuid) {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "t1", "1100", "10", "11000", "ask", "2026-07-08T10:00:00");
        return new BithumbOrderResponse(uuid, "ask", "market", "1100", "done", MARKET,
                null, "10", "0", "0", "0", "27.5", "0", "10", 1, List.of(t));
    }

    private Position openPosition(Long id) {
        Position pos = Position.open(MARKET, new BigDecimal("1000"), new BigDecimal("10"),
                new BigDecimal("970"), new BigDecimal("1150"));
        pos.setId(id);
        return pos;
    }

    private Trade submittedSell(String cid, Long positionId, LocalDateTime orderedAt) {
        return new Trade(20L, cid, positionId, MARKET, TradeType.SELL, "market",
                BigDecimal.ZERO, new BigDecimal("10"), null, null, null,
                TradeStatus.SUBMITTED, -50, "Auto sell signal", orderedAt, null, orderedAt, cid);
    }

    @Test
    void executeSell_prePersistsSubmittedSell_beforePlacingOrder_thenClosesPosition() {
        when(api.placeMarketSellOrder(any(), eq("cid-1"))).thenReturn(sellFill("ex-sell"));
        Position pos = openPosition(5L);

        Signal signal = mock(Signal.class);
        when(signal.getTotalScore()).thenReturn(-50);
        svc.executeSell(MARKET, signal, pos);

        // 순서: SUBMITTED 저장 → 주문 전송, 같은 Trade 가 DONE 으로 갱신
        InOrder order = inOrder(tradeRepo, api);
        order.verify(tradeRepo).save(any(Trade.class));
        order.verify(api).placeMarketSellOrder(any(), eq("cid-1"));
        assertThat(statusAtSave).containsExactly(TradeStatus.SUBMITTED, TradeStatus.DONE);

        ArgumentCaptor<Trade> saved = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getValue().getUuid()).isEqualTo("ex-sell");
        assertThat(saved.getValue().getPositionId()).isEqualTo(5L); // 선영속화 시점부터 포지션 연결
        assertThat(pos.getStatus().name()).isEqualTo("CLOSED");
        verify(breaker).recordOutcome(any());
    }

    @Test
    void executeSell_nullResponse_leavesSubmitted_positionStaysOpen() {
        when(api.placeMarketSellOrder(any(), eq("cid-1"))).thenReturn(null);
        Position pos = openPosition(5L);

        svc.executeSell(MARKET, mock(Signal.class), pos);

        assertThat(statusAtSave).containsExactly(TradeStatus.SUBMITTED);
        assertThat(pos.getStatus().name()).isEqualTo("OPEN"); // 청산 확정 아님 — 스윕이 판정
        verify(posRepo, never()).save(any());
    }

    @Test
    void executeSell_unknownStateResponse_leavesSubmitted() {
        when(api.placeMarketSellOrder(any(), eq("cid-1"))).thenReturn(unknownPartial("oid-s9"));
        Position pos = openPosition(5L);

        svc.executeSell(MARKET, mock(Signal.class), pos);

        assertThat(statusAtSave).containsExactly(TradeStatus.SUBMITTED);
        assertThat(pos.getStatus().name()).isEqualTo("OPEN");
        verify(posRepo, never()).save(any());
    }

    @Test
    void executeSell_blockedWhenUnresolvedSubmittedSellForSamePosition() {
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED))
                .thenReturn(List.of(submittedSell("cid-old", 5L, LocalDateTime.now().minusSeconds(30))));
        Position pos = openPosition(5L);

        svc.executeSell(MARKET, mock(Signal.class), pos);

        // 같은 포지션에 결과 미확인 매도가 있으면 재매도 금지 (이중 매도 방지)
        verify(api, never()).placeMarketSellOrder(any());
        verify(api, never()).placeMarketSellOrder(any(), anyString());
        assertThat(statusAtSave).isEmpty();
    }

    // ==================== 틱 스윕: 매도 정합화 ====================

    @Test
    void sweep_sellConfirmed_marksDone_andClosesLinkedPosition() {
        Trade trade = submittedSell("cid-s7", 5L, LocalDateTime.now().minusSeconds(30));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));
        when(api.getOrderByClientOrderId("cid-s7")).thenReturn(sellFill("ex-s7"));
        Position pos = openPosition(5L);
        when(posRepo.findById(5L)).thenReturn(Optional.of(pos));

        svc.reconcileSubmittedOrders(MARKET);

        assertThat(trade.getStatus()).isEqualTo(TradeStatus.DONE);
        assertThat(trade.getUuid()).isEqualTo("ex-s7");
        assertThat(pos.getStatus().name()).isEqualTo("CLOSED");
        assertThat(pos.getExitPrice()).isEqualByComparingTo("1100");
        verify(posRepo).save(pos);
        verify(breaker).recordOutcome(any()); // 스윕 청산도 서킷브레이커 집계
    }

    @Test
    void sweep_sellConfirmed_positionAlreadyClosed_marksTradeDoneOnly() {
        Trade trade = submittedSell("cid-s8", 5L, LocalDateTime.now().minusSeconds(30));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));
        when(api.getOrderByClientOrderId("cid-s8")).thenReturn(sellFill("ex-s8"));
        Position pos = openPosition(5L);
        pos.close(new BigDecimal("1050"), new BigDecimal("10"),
                me.singingsandhill.calendar.trading.domain.position.CloseReason.STOP_LOSS);
        when(posRepo.findById(5L)).thenReturn(Optional.of(pos));

        svc.reconcileSubmittedOrders(MARKET);

        // 다른 경로(리스크 청산 등)가 이미 닫은 포지션 — Trade 만 정합화, 이중 청산 없음
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.DONE);
        verify(posRepo, never()).save(any());
        verify(breaker, never()).recordOutcome(any());
    }

    // ==================== 루프 통합 ====================

    @Test
    void start_runsStartupSweepOnce() {
        // §8-G: 재시작 직후 미결 주문 스윕 1회 — 재시작 중 발생한 갭 복구
        Trade trade = submitted("cid-16", LocalDateTime.now().minusSeconds(30));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));
        when(api.getOrderByClientOrderId("cid-16")).thenReturn(null);

        svc.start();

        verify(api).getOrderByClientOrderId("cid-16");
    }

    @Test
    void executeTradeLoop_runsSweep_beforeTrading() {
        Trade trade = submitted("cid-14", LocalDateTime.now().minusSeconds(30));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));
        when(api.getOrderByClientOrderId("cid-14")).thenReturn(null);
        when(signals.generateSignal(MARKET)).thenReturn(null);

        svc.start();
        svc.executeTradeLoop();

        // 기동 직후 1회(§8-G) + 루프 시작부 1회
        verify(api, org.mockito.Mockito.times(2)).getOrderByClientOrderId("cid-14");
    }

    @Test
    void executeTradeLoop_sweepFailure_doesNotBlockRiskChecks() {
        Trade trade = submitted("cid-15", LocalDateTime.now().minusSeconds(30));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenReturn(List.of(trade));
        when(api.getOrderByClientOrderId("cid-15")).thenThrow(new RuntimeException("boom"));
        when(signals.generateSignal(MARKET)).thenReturn(null);

        svc.start();
        svc.executeTradeLoop();

        // 스윕 실패가 손절/익절 리스크 체크를 막으면 안 된다
        verify(risk).checkAndExecuteRiskRules(MARKET);
    }
}
