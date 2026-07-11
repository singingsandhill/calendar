package me.singingsandhill.calendar.trading.infrastructure.api;

import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderbookResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-1: PAPER/BACKTEST 모드 가드.
 * LIVE 가 아니면 실주문(privateApi) 을 절대 호출하지 않고 현재가 기반 인메모리 체결을 반환한다.
 */
class BithumbApiClientModeTest {

    private BithumbPublicApi publicApi;
    private BithumbPrivateApi privateApi;
    private BithumbV2OrderApi v2Api;
    private TradingProperties props;
    private BithumbApiClient client;

    @BeforeEach
    void setUp() {
        publicApi = mock(BithumbPublicApi.class);
        privateApi = mock(BithumbPrivateApi.class);
        v2Api = mock(BithumbV2OrderApi.class);
        props = new TradingProperties();
        props.getBot().setMarket("KRW-ADA");
        props.getRisk().setSlippageBuffer(0.005);
        props.getRisk().setTakerFeeRate(0.0025);
        client = new BithumbApiClient(publicApi, privateApi, v2Api, props);

        // 중간가 1000 (ask 1001 / bid 999)
        BithumbOrderbookResponse ob = new BithumbOrderbookResponse(
                "KRW-ADA", 0L, 0.0, 0.0,
                List.of(new BithumbOrderbookResponse.OrderbookUnit(1001.0, 999.0, 1.0, 1.0)));
        when(publicApi.getOrderbook("KRW-ADA", true)).thenReturn(ob);
    }

    @Test
    void paperMode_marketBuy_doesNotCallPrivateApi_andReturnsSimulatedFill() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("100000"));

        verify(privateApi, never()).placeMarketBuyOrder(anyString(), any());
        assertThat(res).isNotNull();
        assertThat(res.uuid()).isNotBlank();
        assertThat(res.state()).isEqualTo("done");
        assertThat(res.trades()).isNotEmpty();
        // 매수 체결가 = 중간가 * (1 + slippage) = 1005
        assertThat(new BigDecimal(res.trades().get(0).price())).isEqualByComparingTo("1005");
        // 수수료 = 100000 * 0.25% = 250
        assertThat(new BigDecimal(res.paidFee())).isEqualByComparingTo("250");
    }

    @Test
    void paperMode_marketSell_doesNotCallPrivateApi_andReturnsSimulatedFill() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);

        BithumbOrderResponse res = client.placeMarketSellOrder(new BigDecimal("100"));

        verify(privateApi, never()).placeMarketSellOrder(anyString(), any());
        assertThat(res).isNotNull();
        assertThat(res.state()).isEqualTo("done");
        assertThat(res.trades()).isNotEmpty();
        // 매도 체결가 = 중간가 * (1 - slippage) = 995
        assertThat(new BigDecimal(res.trades().get(0).price())).isEqualByComparingTo("995");
        // 수수료 = 995 * 100 * 0.25% = 248.75
        assertThat(new BigDecimal(res.paidFee())).isEqualByComparingTo("248.75");
    }

    // §8-A: 모드 게이트 커버리지 — PAPER/BACKTEST 는 취소·지정가·미결조회로도 실계정(privateApi)을 건드리면 안 된다.

    @Test
    void paperMode_cancelAllPendingOrders_doesNotTouchPrivateApi() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);

        client.cancelAllPendingOrders();

        // 실계정 조회/취소가 나가면 안 됨 (emergencyClose 가 PAPER 에서 이 경로를 호출)
        verify(privateApi, never()).getOrders(anyString(), anyString(), anyInt(), anyInt());
        verify(privateApi, never()).cancelOrder(anyString());
    }

    @Test
    void paperMode_getPendingOrders_returnsEmptyWithoutPrivateApi() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);

        assertThat(client.getPendingOrders()).isEmpty();
        verify(privateApi, never()).getOrders(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void paperMode_cancelOrder_doesNotCallPrivateApi() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);

        assertThat(client.cancelOrder("some-uuid")).isNull();
        verify(privateApi, never()).cancelOrder(anyString());
    }

    @Test
    void paperMode_limitBuy_doesNotCallPrivateApi() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);

        client.placeLimitBuyOrder(new BigDecimal("100"), new BigDecimal("1000"));

        verify(privateApi, never()).placeLimitOrder(anyString(), anyString(), any(), any());
    }

    @Test
    void liveMode_marketBuy_delegatesToPrivateApi() {
        props.getBot().setMode(TradingProperties.Bot.Mode.LIVE);
        BithumbOrderResponse delegated = new BithumbOrderResponse(
                "live-uuid", "bid", "price", null, "wait", "KRW-ADA", null,
                null, null, null, null, null, null, null, null, null);
        when(privateApi.placeMarketBuyOrder("KRW-ADA", new BigDecimal("100000"))).thenReturn(delegated);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("100000"));

        verify(privateApi).placeMarketBuyOrder("KRW-ADA", new BigDecimal("100000"));
        assertThat(res).isSameAs(delegated);
    }
}
