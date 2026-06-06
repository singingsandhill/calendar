package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-3 특성화(characterization) 테스트: 트랜잭션 경계 리팩터 후에도 동작이 보존되는지 가드.
 * - 매도 주문 성공 → Trade 저장 + Position 청산 + 서킷브레이커 집계 (영속화는 txTemplate 안에서).
 * - 주문 응답 null → 아무것도 저장하지 않음 (API-first 불변식).
 */
class TradingBotServicePersistenceTest {

    private static final String MARKET = "KRW-ADA";

    private BithumbOrderResponse sellFill() {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "u1", "1100", "10", "11000", "ask", "2026-05-30T10:00:00");
        return new BithumbOrderResponse("u1", "ask", "market", "1100", "done", MARKET,
                null, "10", "0", "0", "0", "27.5", "0", "10", 1, List.of(t));
    }

    private TradingBotService service(BithumbApiClient api, TradeRepository tradeRepo,
                                      PositionRepository posRepo, TradingCircuitBreaker breaker) {
        PlatformTransactionManager txm = mock(PlatformTransactionManager.class);
        return new TradingBotService(null, null, null, null, null, api, tradeRepo, posRepo,
                new TradingProperties(), mock(TradingEventService.class), breaker, null, txm);
    }

    @Test
    void executeSell_successfulOrder_persistsTradeAndClosesPosition() {
        BithumbApiClient api = mock(BithumbApiClient.class);
        when(api.placeMarketSellOrder(any())).thenReturn(sellFill());
        TradeRepository tradeRepo = mock(TradeRepository.class);
        PositionRepository posRepo = mock(PositionRepository.class);
        TradingCircuitBreaker breaker = mock(TradingCircuitBreaker.class);
        TradingBotService svc = service(api, tradeRepo, posRepo, breaker);

        Position pos = Position.open(MARKET, new BigDecimal("1000"), new BigDecimal("10"),
                new BigDecimal("970"), new BigDecimal("1150"));
        Signal signal = mock(Signal.class);
        when(signal.getTotalScore()).thenReturn(-50);

        svc.executeSell(MARKET, signal, pos);

        verify(tradeRepo).save(any(Trade.class));
        verify(posRepo).save(any(Position.class));
        verify(breaker).recordOutcome(any());
        assertThat(pos.getStatus()).isEqualTo(PositionStatus.CLOSED);
        assertThat(pos.getExitPrice()).isEqualByComparingTo("1100");
    }

    @Test
    void executeSell_nullOrderResponse_persistsNothing() {
        BithumbApiClient api = mock(BithumbApiClient.class);
        when(api.placeMarketSellOrder(any())).thenReturn(null);
        TradeRepository tradeRepo = mock(TradeRepository.class);
        PositionRepository posRepo = mock(PositionRepository.class);
        TradingCircuitBreaker breaker = mock(TradingCircuitBreaker.class);
        TradingBotService svc = service(api, tradeRepo, posRepo, breaker);

        Position pos = Position.open(MARKET, new BigDecimal("1000"), new BigDecimal("10"),
                new BigDecimal("970"), new BigDecimal("1150"));
        Signal signal = mock(Signal.class);

        svc.executeSell(MARKET, signal, pos);

        verify(tradeRepo, never()).save(any());
        verify(posRepo, never()).save(any());
        assertThat(pos.getStatus()).isEqualTo(PositionStatus.OPEN);
    }
}
