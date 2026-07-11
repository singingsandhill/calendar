package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * §8-C: 매수 Position 의 수량은 '주문금액/체결가' 유도값이 아니라 실제 체결 수량(executed volume)이어야 한다.
 * 유도값은 수수료·슬리피지를 무시해 장부 수량 > 실잔고 드리프트를 만들고, 이후 매도 시 잔고부족 실패로 이어진다.
 */
class TradingBotServiceExecutedVolumeTest {

    private static final String MARKET = "KRW-ADA";

    /** 248,750 KRW 매수 요청 → 실제로는 1000원에 248개만 체결(수수료/라운딩). 유도값이면 248.75. */
    private BithumbOrderResponse buyFill() {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "t1", "1000", "248", "248000", "bid", "2026-07-06T10:00:00");
        return new BithumbOrderResponse("buy-uuid", "bid", "price", "1000", "done", MARKET,
                null, "248", "0", "0", "0", "621.875", "0", "248", 1, List.of(t));
    }

    private BithumbAccountResponse acct(String currency, String balance) {
        return new BithumbAccountResponse(currency, balance, "0", null, null, "KRW");
    }

    @Test
    void executeBuy_createsPositionWithExecutedVolume_notDerivedFromAmount() {
        BithumbApiClient api = mock(BithumbApiClient.class);
        TradeRepository tradeRepo = mock(TradeRepository.class);
        PositionRepository posRepo = mock(PositionRepository.class);
        TradingCircuitBreaker breaker = mock(TradingCircuitBreaker.class);
        AccountSnapshotRepository snapRepo = mock(AccountSnapshotRepository.class);
        IndicatorService indicators = mock(IndicatorService.class);
        RiskManagementService risk = mock(RiskManagementService.class);
        RebalanceService rebalance = mock(RebalanceService.class);
        PlatformTransactionManager txm = mock(PlatformTransactionManager.class);

        // 서킷브레이커 통과
        when(breaker.isEntryBlocked(any(), any())).thenReturn(false);
        when(snapRepo.findFirstByMarketAndDateRange(any(), any(), any())).thenReturn(Optional.empty());
        when(posRepo.findByMarketAndStatusAndClosedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        // 잔고·가드
        when(api.getKrwBalance()).thenReturn(acct("KRW", "1000000"));
        when(api.getCurrentPrice()).thenReturn(1000.0);
        when(posRepo.findByMarketAndStatus(any(), any())).thenReturn(List.of());
        when(api.getCoinBalance()).thenReturn(acct("ADA", "0"));
        // ATR 계산 실패 → 기본 비율 0.25 → 주문액 250,000 → 슬리피지 0.5% → 248,750
        when(indicators.calculateATRPercent(any())).thenReturn(null);
        when(api.placeMarketBuyOrder(any())).thenReturn(buyFill());
        when(risk.calculateStopLossPrice(any())).thenReturn(new BigDecimal("985"));
        when(risk.calculateTakeProfitPrice(any())).thenReturn(new BigDecimal("1030"));

        TradingBotService svc = new TradingBotService(
                null, null, indicators, risk, rebalance, api, tradeRepo, posRepo,
                new TradingProperties(), mock(TradingEventService.class), breaker, snapRepo, txm);

        Signal signal = mock(Signal.class);
        when(signal.getTotalScore()).thenReturn(50);

        svc.executeBuy(MARKET, signal);

        ArgumentCaptor<Position> pos = ArgumentCaptor.forClass(Position.class);
        verify(posRepo).save(pos.capture());
        // 실제 체결 수량 248 (유도값 248.75 아님)
        assertThat(pos.getValue().getEntryVolume()).isEqualByComparingTo("248");
    }
}
