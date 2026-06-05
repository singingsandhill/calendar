package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-3: 리밸런싱 회계 정합.
 * - 매도: OPEN 포지션을 FIFO 로 청산, 수익 포지션만(적자 청산 방지), 목표량 도달 시 중단.
 * - 매수: 실주문 후 추적 Position(SL/TP) 생성 → 리스크 루프가 보호.
 */
class RebalanceServiceAccountingTest {

    private static final String MARKET = "KRW-ADA";
    private static final BigDecimal PRICE = new BigDecimal("1100");

    private Position openPosition(String entryPrice, String volume, LocalDateTime openedAt) {
        BigDecimal ep = new BigDecimal(entryPrice);
        BigDecimal vol = new BigDecimal(volume);
        return new Position(null, MARKET, PositionStatus.OPEN, ep, vol, ep.multiply(vol),
                null, null, null, null, null, null, null, null, ep, false, null,
                openedAt, null, openedAt, BigDecimal.ZERO, null, null);
    }

    private RebalanceService service(PositionRepository posRepo, BithumbApiClient api,
                                     RiskManagementService risk, TradeRepository tradeRepo) {
        TradingProperties props = new TradingProperties();
        return new RebalanceService(api, null, props, tradeRepo, posRepo,
                mock(TradingEventService.class), risk, mock(PlatformTransactionManager.class));
    }

    @Test
    void sell_closesProfitablePositions_skipsUnprofitable() {
        Position profitable = openPosition("1000", "10", LocalDateTime.of(2026, 5, 30, 9, 0));
        Position unprofitable = openPosition("1200", "10", LocalDateTime.of(2026, 5, 30, 10, 0));
        PositionRepository posRepo = mock(PositionRepository.class);
        when(posRepo.findByMarketAndStatus(MARKET, PositionStatus.OPEN))
                .thenReturn(List.of(unprofitable, profitable)); // 정렬 전 순서
        RiskManagementService risk = mock(RiskManagementService.class);
        RebalanceService svc = service(posRepo, null, risk, null);

        BigDecimal sold = svc.sellByClosingProfitablePositions(MARKET, new BigDecimal("100"), PRICE);

        verify(risk).closePosition(profitable, PRICE, CloseReason.REBALANCE);
        verify(risk, never()).closePosition(eq(unprofitable), any(), any());
        assertThat(sold).isEqualByComparingTo("10");
    }

    @Test
    void sell_fifoOldestFirst_stopsAtTargetVolume() {
        Position oldest = openPosition("1000", "10", LocalDateTime.of(2026, 5, 30, 9, 0));
        Position newest = openPosition("1000", "10", LocalDateTime.of(2026, 5, 30, 10, 0));
        PositionRepository posRepo = mock(PositionRepository.class);
        when(posRepo.findByMarketAndStatus(MARKET, PositionStatus.OPEN))
                .thenReturn(List.of(newest, oldest)); // 정렬 전 순서
        RiskManagementService risk = mock(RiskManagementService.class);
        RebalanceService svc = service(posRepo, null, risk, null);

        // 목표 5 < 한 포지션(10) → 가장 오래된 것 하나만 청산 후 중단
        BigDecimal sold = svc.sellByClosingProfitablePositions(MARKET, new BigDecimal("5"), PRICE);

        verify(risk).closePosition(oldest, PRICE, CloseReason.REBALANCE);
        verify(risk, never()).closePosition(eq(newest), any(), any());
        assertThat(sold).isEqualByComparingTo("10");
    }

    @Test
    void sell_noOpenPositions_sellsNothing() {
        PositionRepository posRepo = mock(PositionRepository.class);
        when(posRepo.findByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(List.of());
        RiskManagementService risk = mock(RiskManagementService.class);
        RebalanceService svc = service(posRepo, null, risk, null);

        BigDecimal sold = svc.sellByClosingProfitablePositions(MARKET, new BigDecimal("100"), PRICE);

        assertThat(sold).isEqualByComparingTo("0");
        verify(risk, never()).closePosition(any(), any(), any());
    }

    @Test
    void buy_usesExecutedPriceFromResponse_notParameterPrice() {
        // #4: 응답 trades 의 실체결가(1005)로 entryPrice/volume/SL/TP 계산 (파라미터 1000 아님)
        BithumbApiClient api = mock(BithumbApiClient.class);
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "u", "1005", "10", "10050", "bid", "2026-05-30T10:00:00");
        BithumbOrderResponse resp = new BithumbOrderResponse(
                "uuid-2", "bid", "price", null, "done", MARKET, null,
                null, null, null, null, "0", null, null, 1, List.of(t));
        when(api.placeMarketBuyOrder(new BigDecimal("50000"))).thenReturn(resp);
        RiskManagementService risk = mock(RiskManagementService.class);
        when(risk.calculateStopLossPrice(new BigDecimal("1005"))).thenReturn(new BigDecimal("990"));
        when(risk.calculateTakeProfitPrice(new BigDecimal("1005"))).thenReturn(new BigDecimal("1035"));
        PositionRepository posRepo = mock(PositionRepository.class);
        TradeRepository tradeRepo = mock(TradeRepository.class);
        RebalanceService svc = service(posRepo, api, risk, tradeRepo);

        boolean ok = svc.buyAndOpenPosition(MARKET, new BigDecimal("50000"), new BigDecimal("1000"));

        assertThat(ok).isTrue();
        ArgumentCaptor<Position> cap = ArgumentCaptor.forClass(Position.class);
        verify(posRepo).save(cap.capture());
        Position saved = cap.getValue();
        assertThat(saved.getEntryPrice()).isEqualByComparingTo("1005"); // 실체결가
        assertThat(saved.getEntryVolume().doubleValue()).isCloseTo(49.751, org.assertj.core.data.Offset.offset(0.01)); // 50000/1005
    }

    @Test
    void buy_opensTrackedPositionWithStops() {
        BithumbApiClient api = mock(BithumbApiClient.class);
        BithumbOrderResponse resp = new BithumbOrderResponse(
                "uuid-1", "bid", "price", null, "done", MARKET, null,
                null, null, null, null, "125", null, null, null, null);
        when(api.placeMarketBuyOrder(new BigDecimal("50000"))).thenReturn(resp);
        RiskManagementService risk = mock(RiskManagementService.class);
        when(risk.calculateStopLossPrice(new BigDecimal("1000"))).thenReturn(new BigDecimal("970"));
        when(risk.calculateTakeProfitPrice(new BigDecimal("1000"))).thenReturn(new BigDecimal("1150"));
        PositionRepository posRepo = mock(PositionRepository.class);
        TradeRepository tradeRepo = mock(TradeRepository.class);
        RebalanceService svc = service(posRepo, api, risk, tradeRepo);

        boolean ok = svc.buyAndOpenPosition(MARKET, new BigDecimal("50000"), new BigDecimal("1000"));

        assertThat(ok).isTrue();
        ArgumentCaptor<Position> cap = ArgumentCaptor.forClass(Position.class);
        verify(posRepo).save(cap.capture());
        Position saved = cap.getValue();
        assertThat(saved.getStopLossPrice()).isEqualByComparingTo("970");
        assertThat(saved.getTakeProfitPrice()).isEqualByComparingTo("1150");
        assertThat(saved.getEntryVolume()).isEqualByComparingTo("50"); // 50000 / 1000
        verify(tradeRepo).save(any(Trade.class));
    }
}
