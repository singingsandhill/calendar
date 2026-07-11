package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-3: enabled=false(킬스위치)는 수동 매수/매도 실주문을 막아야 한다.
 * P1-4: 수동 매수는 진입 리스크 가드(서킷브레이커·물타기·노출상한)를 우회하면 안 된다.
 * (긴급청산 emergencyClose 는 안전 청산이라 게이트 대상 아님.)
 */
class TradingBotServiceManualGuardTest {

    private static final String MARKET = "KRW-ADA";

    private BithumbApiClient api = mock(BithumbApiClient.class);
    private PositionRepository posRepo = mock(PositionRepository.class);
    private AccountSnapshotRepository snapRepo = mock(AccountSnapshotRepository.class);
    private TradingCircuitBreaker breaker = mock(TradingCircuitBreaker.class);
    private RiskManagementService risk = mock(RiskManagementService.class);
    private TradingProperties props = new TradingProperties();

    private TradingBotService service() {
        props.getBot().setMarket(MARKET);
        return new TradingBotService(null, null, null, risk, null, api,
                mock(me.singingsandhill.calendar.trading.domain.trade.TradeRepository.class), posRepo,
                props, mock(TradingEventService.class), breaker, snapRepo,
                mock(PlatformTransactionManager.class));
    }

    private BithumbAccountResponse acct(String balance) {
        return new BithumbAccountResponse("KRW", balance, "0", null, null, "KRW");
    }

    private BithumbOrderResponse buyFill() {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "t1", "1000", "9", "9000", "bid", "2026-07-08T10:00:00");
        return new BithumbOrderResponse("buy-uuid", "bid", "price", "1000", "done", MARKET,
                null, "9", "0", "0", "0", "25", "0", "9", 1, List.of(t));
    }

    // ---- P0-3: 킬스위치 ----

    @Test
    void manualBuy_tradingDisabled_rejectsWithoutOrder() {
        props.getBot().setEnabled(false);
        TradingBotService svc = service();

        assertThat(svc.manualBuy(new BigDecimal("10000"))).isFalse();
        verify(api, never()).placeMarketBuyOrder(any());
    }

    @Test
    void manualSell_tradingDisabled_rejectsWithoutOrder() {
        props.getBot().setEnabled(false);
        TradingBotService svc = service();

        assertThat(svc.manualSell(new BigDecimal("5"))).isFalse();
        verify(api, never()).placeMarketSellOrder(any());
    }

    // ---- P1-4: 진입 리스크 가드 ----

    @Test
    void manualBuy_circuitBreakerBlocks_rejectsWithoutOrder() {
        props.getBot().setEnabled(true);
        when(breaker.isEntryBlocked(any(), any())).thenReturn(true);
        when(snapRepo.findFirstByMarketAndDateRange(any(), any(), any())).thenReturn(Optional.empty());
        when(posRepo.findByMarketAndStatusAndClosedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        TradingBotService svc = service();

        assertThat(svc.manualBuy(new BigDecimal("10000"))).isFalse();
        verify(api, never()).placeMarketBuyOrder(any());
    }

    @Test
    void manualBuy_averagingDownBlocks_rejectsWithoutOrder() {
        props.getBot().setEnabled(true);
        when(breaker.isEntryBlocked(any(), any())).thenReturn(false);
        when(snapRepo.findFirstByMarketAndDateRange(any(), any(), any())).thenReturn(Optional.empty());
        when(posRepo.findByMarketAndStatusAndClosedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        when(api.getCurrentPrice()).thenReturn(1000.0);
        // 진입가 1100 > 현재가 1000 → 손실 포지션 보유 → 물타기 차단
        Position losing = Position.open(MARKET, new BigDecimal("1100"), new BigDecimal("1"),
                new BigDecimal("1083"), new BigDecimal("1133"));
        when(posRepo.findByMarketAndStatus(any(), any())).thenReturn(List.of(losing));
        TradingBotService svc = service();

        assertThat(svc.manualBuy(new BigDecimal("10000"))).isFalse();
        verify(api, never()).placeMarketBuyOrder(any());
    }

    // ---- 정상 경로 보존 ----

    @Test
    void manualBuy_enabledAndGuardsPass_placesOrder() {
        props.getBot().setEnabled(true);
        when(breaker.isEntryBlocked(any(), any())).thenReturn(false);
        when(snapRepo.findFirstByMarketAndDateRange(any(), any(), any())).thenReturn(Optional.empty());
        when(posRepo.findByMarketAndStatusAndClosedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        when(api.getCurrentPrice()).thenReturn(1000.0);
        when(posRepo.findByMarketAndStatus(any(), any())).thenReturn(List.of());
        when(api.getCoinBalance()).thenReturn(acct("0"));
        when(api.getKrwBalance()).thenReturn(acct("1000000"));
        when(api.placeMarketBuyOrder(any())).thenReturn(buyFill());
        when(risk.calculateStopLossPrice(any())).thenReturn(new BigDecimal("985"));
        when(risk.calculateTakeProfitPrice(any())).thenReturn(new BigDecimal("1030"));
        TradingBotService svc = service();

        assertThat(svc.manualBuy(new BigDecimal("9000"))).isTrue();
        verify(api).placeMarketBuyOrder(any());
        verify(posRepo).save(any(Position.class));
    }
}
