package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
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
 * 신호 기반 체결에만 {@code signal_id} 를 연결하고, 매수에는 실제 적용된 주문 비중을 남긴다
 * (ADR trading/observability/0002).
 *
 * <p>이전에는 Trade 에 {@code signal_score} 정수 하나만 있어서 "이 체결이 어느 신호에서 나왔는가" 를
 * 타임스탬프 근사로만 추정할 수 있었다. 게다가 {@code trading_signals.executed} 는 한 번도 true 로
 * 세팅되지 않는 죽은 컬럼이라 신호 쪽에서도 역추적이 불가능했다.
 *
 * <p>주문 비중은 {@code calculateDynamicOrderRatio} 가 계산해 쓰고 버려서, 포지션 사이징 결정을
 * 사후에 DB 로 재구성할 수 없었다. 특히 보간 구간의 결과는 double 이라 그대로 저장하면
 * {@code 0.29999999999999993} 같은 값이 남는다 — 저장 전 4자리 반올림이 필요한 이유다.
 *
 * <p>리스크 청산·리밸런싱·수동 매매는 신호가 원인이 아니므로 {@code signal_id} 를 <b>일부러</b>
 * null 로 둔다. null 자체가 "신호로 난 체결이 아니다" 라는 정보다.
 */
class TradingBotServiceSignalLinkTest {

    private static final String MARKET = "KRW-ADA";
    private static final long SIGNAL_ID = 77L;

    private BithumbOrderResponse buyFill() {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "t1", "1000", "248", "248000", "bid", "2026-08-06T10:00:00");
        return new BithumbOrderResponse("buy-uuid", "bid", "price", "1000", "done", MARKET,
                null, "248", "0", "0", "0", "621.875", "0", "248", 1, List.of(t));
    }

    private BithumbOrderResponse sellFill() {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "u1", "1100", "10", "11000", "ask", "2026-08-06T10:00:00");
        return new BithumbOrderResponse("sell-uuid", "ask", "market", "1100", "done", MARKET,
                null, "10", "0", "0", "0", "27.5", "0", "10", 1, List.of(t));
    }

    private BithumbAccountResponse acct(String currency, String balance) {
        return new BithumbAccountResponse(currency, balance, "0", null, null, "KRW");
    }

    private Signal signalWithId(Long id, int score) {
        Signal signal = mock(Signal.class);
        when(signal.getId()).thenReturn(id);
        when(signal.getTotalScore()).thenReturn(score);
        return signal;
    }

    /** 매수 경로를 끝까지 통과시키고, 저장된 Trade 를 돌려준다. atrPercent=null 이면 기본 비율 0.25. */
    private Trade captureBuyTrade(Signal signal, BigDecimal atrPercent) {
        BithumbApiClient api = mock(BithumbApiClient.class);
        TradeRepository tradeRepo = mock(TradeRepository.class);
        PositionRepository posRepo = mock(PositionRepository.class);
        TradingCircuitBreaker breaker = mock(TradingCircuitBreaker.class);
        AccountSnapshotRepository snapRepo = mock(AccountSnapshotRepository.class);
        IndicatorService indicators = mock(IndicatorService.class);
        RiskManagementService risk = mock(RiskManagementService.class);

        when(breaker.isEntryBlocked(any(), any())).thenReturn(false);
        when(snapRepo.findFirstByMarketAndDateRange(any(), any(), any())).thenReturn(Optional.empty());
        when(posRepo.findByMarketAndStatusAndClosedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        when(api.getKrwBalance()).thenReturn(acct("KRW", "1000000"));
        when(api.getCurrentPrice()).thenReturn(1000.0);
        when(posRepo.findByMarketAndStatus(any(), any())).thenReturn(List.of());
        when(api.getCoinBalance()).thenReturn(acct("ADA", "0"));
        when(indicators.calculateATRPercent(any())).thenReturn(atrPercent);
        when(api.placeMarketBuyOrder(any())).thenReturn(buyFill());
        when(risk.calculateStopLossPrice(any())).thenReturn(new BigDecimal("985"));
        when(risk.calculateTakeProfitPrice(any())).thenReturn(new BigDecimal("1030"));

        TradingBotService svc = new TradingBotService(
                null, null, indicators, risk, mock(RebalanceService.class), api, tradeRepo, posRepo,
                new TradingProperties(), mock(TradingEventService.class), breaker, snapRepo,
                mock(PlatformTransactionManager.class));

        svc.executeBuy(MARKET, signal);

        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void executeBuy_linksTradeToSignal() {
        Trade trade = captureBuyTrade(signalWithId(SIGNAL_ID, 50), null);

        assertThat(trade.getSignalId()).isEqualTo(SIGNAL_ID);
        assertThat(trade.getSignalReason()).isEqualTo("Auto buy signal");
    }

    @Test
    void executeBuy_recordsDefaultRatioWhenAtrUnavailable() {
        Trade trade = captureBuyTrade(signalWithId(SIGNAL_ID, 50), null);

        assertThat(trade.getOrderRatio()).isEqualByComparingTo("0.25");
    }

    @Test
    void executeBuy_recordsMinRatioOnHighVolatility() {
        Trade trade = captureBuyTrade(signalWithId(SIGNAL_ID, 50), new BigDecimal("3.5"));

        assertThat(trade.getOrderRatio()).isEqualByComparingTo("0.15");
    }

    @Test
    void executeBuy_scalesInterpolatedRatioSoItIsStorable() {
        // ATR 1.5% → 0.35 - ((1.5-1)/2)*0.20. double 로는 0.29999999999999993 이 나온다.
        // setScale(4, HALF_UP) 없이 그대로 저장하면 이 값이 DB 에 남고 구간 집계가 지저분해진다.
        Trade trade = captureBuyTrade(signalWithId(SIGNAL_ID, 50), new BigDecimal("1.5"));

        assertThat(trade.getOrderRatio()).isEqualByComparingTo("0.30");
        assertThat(trade.getOrderRatio().scale()).isEqualTo(4);
    }

    @Test
    void executeSell_linksTradeToSignal() {
        BithumbApiClient api = mock(BithumbApiClient.class);
        when(api.placeMarketSellOrder(any())).thenReturn(sellFill());
        TradeRepository tradeRepo = mock(TradeRepository.class);
        PositionRepository posRepo = mock(PositionRepository.class);

        TradingBotService svc = new TradingBotService(
                null, null, null, null, null, api, tradeRepo, posRepo,
                new TradingProperties(), mock(TradingEventService.class),
                mock(TradingCircuitBreaker.class), null, mock(PlatformTransactionManager.class));

        Position pos = Position.open(MARKET, new BigDecimal("1000"), new BigDecimal("10"),
                new BigDecimal("970"), new BigDecimal("1150"));

        svc.executeSell(MARKET, signalWithId(88L, -50), pos);

        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo).save(captor.capture());
        assertThat(captor.getValue().getSignalId()).isEqualTo(88L);
        // 매도는 position.getEntryVolume() 으로 크기가 정해지므로 주문 비중 개념이 없다.
        assertThat(captor.getValue().getOrderRatio()).isNull();
    }

    @Test
    void manualSell_leavesSignalIdNull() {
        BithumbApiClient api = mock(BithumbApiClient.class);
        when(api.placeMarketSellOrder(any())).thenReturn(sellFill());
        when(api.getCoinBalance()).thenReturn(acct("ADA", "10"));
        when(api.getCurrentPrice()).thenReturn(1100.0);
        TradeRepository tradeRepo = mock(TradeRepository.class);
        PositionRepository posRepo = mock(PositionRepository.class);
        when(posRepo.findOpenPositionsByMarket(any())).thenReturn(List.of());

        // P0-3 킬스위치를 열어야 수동 주문이 실제로 나간다 (bot.enabled 기본값은 false).
        TradingProperties properties = new TradingProperties();
        properties.getBot().setEnabled(true);

        TradingBotService svc = new TradingBotService(
                null, null, null, null, null, api, tradeRepo, posRepo,
                properties, mock(TradingEventService.class),
                mock(TradingCircuitBreaker.class), null, mock(PlatformTransactionManager.class));

        svc.manualSell(new BigDecimal("10"));

        ArgumentCaptor<Trade> captor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo).save(captor.capture());
        // 운영자 조작은 신호가 원인이 아니다 — null 이 곧 그 정보다.
        assertThat(captor.getValue().getSignalId()).isNull();
    }
}
