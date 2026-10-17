package me.singingsandhill.calendar.trading.infrastructure.api;

import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * V1 LIVE 시장가 주문의 체결 정보 정규화.
 * POST /v1/orders 응답은 접수 시점 스냅샷이라 trades 가 없고 paid_fee 가 0 이다 — 그대로 올려보내면
 * 수수료가 0 으로 장부에 남고(운영 30일 69건 전부), 재시도가 없는 리밸런싱 매수는 체결가 대신 호가 mid 를
 * 진입가로 기록한다. GET /v1/order 재조회로 trades·paid_fee 가 채워진 상세를 반환해야 한다(v2 어댑터와 동일 형태).
 */
class BithumbApiClientV1FillTest {

    private BithumbPrivateApi privateApi;
    private BithumbApiClient client;

    @BeforeEach
    void setUp() {
        privateApi = mock(BithumbPrivateApi.class);
        TradingProperties props = new TradingProperties();
        props.getBot().setMarket("KRW-ADA");
        props.getBot().setMode(TradingProperties.Bot.Mode.LIVE);
        client = new BithumbApiClient(mock(BithumbPublicApi.class), privateApi,
                mock(BithumbV2OrderApi.class), props);
        client.setFillRequeryBackoffMillis(0);
    }

    private BithumbOrderResponse accepted(String uuid, String side) {
        return new BithumbOrderResponse(uuid, side, "price", null, "wait", "KRW-ADA", null,
                null, null, null, null, "0", null, "0", 0, null);
    }

    private BithumbOrderResponse filled(String uuid, String side, String price, String volume, String paidFee) {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                "KRW-ADA", uuid + "-t1", price, volume, null, side, null);
        return new BithumbOrderResponse(uuid, side, "price", null, "done", "KRW-ADA", null,
                null, null, null, null, paidFee, null, volume, 1, List.of(t));
    }

    @Test
    void buy_acceptedWithoutTrades_returnsRequeriedFill_postSentOnce() {
        BithumbOrderResponse placed = accepted("uuid-1", "bid");
        BithumbOrderResponse detail = filled("uuid-1", "bid", "338", "31.0", "4.2");
        when(privateApi.placeMarketBuyOrder("KRW-ADA", new BigDecimal("10471"))).thenReturn(placed);
        when(privateApi.getOrder("uuid-1")).thenReturn(detail);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("10471"));

        assertThat(res).isSameAs(detail);
        assertThat(res.paidFee()).isEqualTo("4.2");
        verify(privateApi, times(1)).placeMarketBuyOrder(anyString(), any());
    }

    @Test
    void buy_firstDetailHasNoTradesYet_keepsRequerying() {
        BithumbOrderResponse placed = accepted("uuid-2", "bid");
        BithumbOrderResponse notYet = accepted("uuid-2", "bid");
        BithumbOrderResponse detail = filled("uuid-2", "bid", "338", "31.0", "4.2");
        when(privateApi.placeMarketBuyOrder("KRW-ADA", new BigDecimal("10471"))).thenReturn(placed);
        when(privateApi.getOrder("uuid-2")).thenReturn(notYet, detail);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("10471"));

        assertThat(res).isSameAs(detail);
        verify(privateApi, times(2)).getOrder("uuid-2");
    }

    @Test
    void buy_requeryNeverFindsTrades_returnsOriginalResponse() {
        BithumbOrderResponse placed = accepted("uuid-3", "bid");
        when(privateApi.placeMarketBuyOrder("KRW-ADA", new BigDecimal("10471"))).thenReturn(placed);
        when(privateApi.getOrder("uuid-3")).thenReturn(null);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("10471"));

        // 기존 동작 유지 — 상위 서비스의 체결가 재시도가 이어받는다
        assertThat(res).isSameAs(placed);
        verify(privateApi, times(3)).getOrder("uuid-3");
        verify(privateApi, times(1)).placeMarketBuyOrder(anyString(), any());
    }

    @Test
    void buy_responseAlreadyHasTrades_noRequery() {
        BithumbOrderResponse placed = filled("uuid-4", "bid", "338", "31.0", "4.2");
        when(privateApi.placeMarketBuyOrder("KRW-ADA", new BigDecimal("10471"))).thenReturn(placed);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("10471"));

        assertThat(res).isSameAs(placed);
        verify(privateApi, never()).getOrder(anyString());
    }

    @Test
    void sell_acceptedWithoutTrades_returnsRequeriedFill_postSentOnce() {
        BithumbOrderResponse placed = accepted("uuid-5", "ask");
        BithumbOrderResponse detail = filled("uuid-5", "ask", "343", "31.0", "4.3");
        when(privateApi.placeMarketSellOrder("KRW-ADA", new BigDecimal("31.0"))).thenReturn(placed);
        when(privateApi.getOrder("uuid-5")).thenReturn(detail);

        BithumbOrderResponse res = client.placeMarketSellOrder(new BigDecimal("31.0"));

        assertThat(res).isSameAs(detail);
        assertThat(res.paidFee()).isEqualTo("4.3");
        verify(privateApi, times(1)).placeMarketSellOrder(anyString(), any());
    }

    @Test
    void cidPath_acceptedWithoutTrades_alsoNormalized() {
        TradingProperties props = new TradingProperties();
        props.getBot().setMarket("KRW-ADA");
        props.getBot().setMode(TradingProperties.Bot.Mode.LIVE);
        props.getBithumb().setClientOrderIdEnabled(true);
        client = new BithumbApiClient(mock(BithumbPublicApi.class), privateApi,
                mock(BithumbV2OrderApi.class), props);
        client.setFillRequeryBackoffMillis(0);
        BithumbOrderResponse placed = accepted("uuid-6", "bid");
        BithumbOrderResponse detail = filled("uuid-6", "bid", "338", "31.0", "4.2");
        when(privateApi.placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString())).thenReturn(placed);
        when(privateApi.getOrder("uuid-6")).thenReturn(detail);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("10471"));

        assertThat(res).isSameAs(detail);
        verify(privateApi, never()).getOrderByClientOrderId(anyString());
    }
}
