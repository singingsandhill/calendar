package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * #3: 수동 매매도 Position 을 생성/청산해 계좌-포지션 정합성을 유지.
 */
class TradingBotServiceManualTest {

    private static final String MARKET = "KRW-ADA";

    private BithumbOrderResponse fill(String price, String volume, String side) {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "u", price, volume, "0", side, "2026-05-30T10:00:00");
        return new BithumbOrderResponse("uuid", side, "market", price, "done", MARKET, null,
                volume, null, null, null, "0", null, null, 1, List.of(t));
    }

    private TradingBotService service(BithumbApiClient api, RiskManagementService risk,
                                      TradeRepository tradeRepo, PositionRepository posRepo,
                                      TradingCircuitBreaker breaker) {
        TradingProperties props = new TradingProperties();
        props.getBot().setMarket(MARKET);
        return new TradingBotService(null, null, null, risk, null, api, tradeRepo, posRepo,
                props, mock(TradingEventService.class), breaker, null, mock(PlatformTransactionManager.class));
    }

    private Position posOpenedAt(LocalDateTime openedAt, String entryPrice, String volume) {
        BigDecimal e = new BigDecimal(entryPrice);
        BigDecimal v = new BigDecimal(volume);
        return new Position(null, MARKET, PositionStatus.OPEN, e, v, e.multiply(v),
                null, null, null, null, null, null, null, null, e, false, null,
                openedAt, null, openedAt, BigDecimal.ZERO, null, null);
    }

    @Test
    void manualBuy_createsTrackedPositionWithStops() {
        BithumbApiClient api = mock(BithumbApiClient.class);
        when(api.placeMarketBuyOrder(new BigDecimal("50000"))).thenReturn(fill("1000", "50", "bid"));
        RiskManagementService risk = mock(RiskManagementService.class);
        // extractExecutedPrice 는 scale-8 BigDecimal 반환 → equals 매칭 회피 위해 any()
        when(risk.calculateStopLossPrice(any())).thenReturn(new BigDecimal("985"));
        when(risk.calculateTakeProfitPrice(any())).thenReturn(new BigDecimal("1030"));
        PositionRepository posRepo = mock(PositionRepository.class);
        TradeRepository tradeRepo = mock(TradeRepository.class);
        TradingBotService svc = service(api, risk, tradeRepo, posRepo, mock(TradingCircuitBreaker.class));

        boolean ok = svc.manualBuy(new BigDecimal("50000"));

        assertThat(ok).isTrue();
        ArgumentCaptor<Position> cap = ArgumentCaptor.forClass(Position.class);
        verify(posRepo).save(cap.capture());
        assertThat(cap.getValue().getStopLossPrice()).isEqualByComparingTo("985");
        assertThat(cap.getValue().getTakeProfitPrice()).isEqualByComparingTo("1030");
    }

    @Test
    void reconcile_closesOldestOpenPositionsFifoUpToVolume() {
        PositionRepository posRepo = mock(PositionRepository.class);
        Position oldest = posOpenedAt(LocalDateTime.of(2026, 5, 30, 9, 0), "1000", "10");
        Position newest = posOpenedAt(LocalDateTime.of(2026, 5, 30, 10, 0), "1000", "10");
        when(posRepo.findByMarketAndStatus(MARKET, PositionStatus.OPEN)).thenReturn(List.of(newest, oldest));
        TradingBotService svc = service(mock(BithumbApiClient.class), mock(RiskManagementService.class),
                mock(TradeRepository.class), posRepo, mock(TradingCircuitBreaker.class));

        // 매도량 10 → 가장 오래된 포지션 1개만 청산, 최신 포지션은 OPEN 유지
        svc.reconcilePositionsAfterManualSell(MARKET, new BigDecimal("10"), new BigDecimal("1000"));

        assertThat(oldest.getStatus()).isEqualTo(PositionStatus.CLOSED);
        assertThat(newest.getStatus()).isEqualTo(PositionStatus.OPEN);
    }
}
