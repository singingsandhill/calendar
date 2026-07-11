package me.singingsandhill.calendar.trading.infrastructure.api;

import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderbookResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
 * P0-2 / Phase 0b: client_order_id 멱등 재조회 (파사드 레벨).
 * clientOrderIdEnabled + LIVE 에서 주문 응답이 null(타임아웃 모사) 이면 무작정 재전송하지 않고
 * client_order_id 로 재조회해 실제 접수 여부를 확인한다 — 발견되면 복구 주문 반환, 없으면 null.
 * 플래그 OFF(기본)면 기존 동작(2-인자 위임)과 100% 동일.
 */
class BithumbApiClientIdempotencyTest {

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
        props.getBot().setMode(TradingProperties.Bot.Mode.LIVE);
        props.getBithumb().setClientOrderIdEnabled(true);
        client = new BithumbApiClient(publicApi, privateApi, v2Api, props);

        BithumbOrderbookResponse ob = new BithumbOrderbookResponse(
                "KRW-ADA", 0L, 0.0, 0.0,
                List.of(new BithumbOrderbookResponse.OrderbookUnit(1001.0, 999.0, 1.0, 1.0)));
        when(publicApi.getOrderbook("KRW-ADA", true)).thenReturn(ob);
    }

    private BithumbOrderResponse order(String uuid, String state) {
        return new BithumbOrderResponse(uuid, "bid", "price", null, state, "KRW-ADA", null,
                null, null, null, null, null, null, null, null, null);
    }

    @Test
    void flagOn_buyResponseNonNull_returnsItWithoutReconcile() {
        BithumbOrderResponse placed = order("uuid-1", "wait");
        when(privateApi.placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString())).thenReturn(placed);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"));

        assertThat(res).isSameAs(placed);
        verify(privateApi, never()).getOrderByClientOrderId(anyString());
    }

    @Test
    void flagOn_buyResponseNull_reconcileFindsOrder_returnsRecovered() {
        when(privateApi.placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString())).thenReturn(null);
        BithumbOrderResponse recovered = order("uuid-2", "done");
        when(privateApi.getOrderByClientOrderId(anyString())).thenReturn(recovered);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"));

        // 핵심: null 이 아니라 재조회로 복구한 주문을 반환한다 (이중체결 방지)
        assertThat(res).isSameAs(recovered);
        // 재전송 없음: 주문 전송은 정확히 1회
        verify(privateApi, times(1)).placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString());
    }

    @Test
    void flagOn_buyResponseNull_reconcileEmpty_returnsNull_noResend() {
        when(privateApi.placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString())).thenReturn(null);
        when(privateApi.getOrderByClientOrderId(anyString())).thenReturn(null);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"));

        assertThat(res).isNull();
        verify(privateApi, times(1)).placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString());
    }

    @Test
    void flagOn_buy_clientOrderIdIsStableAndValidFormat() {
        when(privateApi.placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString())).thenReturn(null);
        when(privateApi.getOrderByClientOrderId(anyString())).thenReturn(null);

        client.placeMarketBuyOrder(new BigDecimal("6000"));

        ArgumentCaptor<String> placeCid = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> lookupCid = ArgumentCaptor.forClass(String.class);
        verify(privateApi).placeMarketBuyOrder(eq("KRW-ADA"), any(), placeCid.capture());
        verify(privateApi).getOrderByClientOrderId(lookupCid.capture());

        // 재조회는 방금 시도한 그 주문을 조회해야 함
        assertThat(lookupCid.getValue()).isEqualTo(placeCid.getValue());
        // Bithumb 제약: 1~36자, [A-Za-z0-9-_]
        assertThat(placeCid.getValue()).matches("^[A-Za-z0-9_-]{1,36}$");
    }

    @Test
    void flagOn_sellResponseNull_reconcileFindsOrder_returnsRecovered() {
        when(privateApi.placeMarketSellOrder(eq("KRW-ADA"), any(), anyString())).thenReturn(null);
        BithumbOrderResponse recovered = order("uuid-3", "done");
        when(privateApi.getOrderByClientOrderId(anyString())).thenReturn(recovered);

        BithumbOrderResponse res = client.placeMarketSellOrder(new BigDecimal("5"));

        assertThat(res).isSameAs(recovered);
        verify(privateApi, times(1)).placeMarketSellOrder(eq("KRW-ADA"), any(), anyString());
    }

    @Test
    void flagOff_buy_delegatesToTwoArg_noClientOrderId_noReconcile() {
        props.getBithumb().setClientOrderIdEnabled(false);
        BithumbOrderResponse placed = order("uuid-4", "wait");
        when(privateApi.placeMarketBuyOrder("KRW-ADA", new BigDecimal("6000"))).thenReturn(placed);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"));

        assertThat(res).isSameAs(placed);
        verify(privateApi).placeMarketBuyOrder("KRW-ADA", new BigDecimal("6000"));
        verify(privateApi, never()).placeMarketBuyOrder(anyString(), any(), anyString());
        verify(privateApi, never()).getOrderByClientOrderId(anyString());
    }

    // Phase 1: 버전 라우팅 — order-api-version 에 따라 v1(privateApi) 또는 v2(v2Api) 로 위임.

    @Test
    void v2Version_buy_routesToV2Api_notPrivateApi() {
        props.getBithumb().setOrderApiVersion(TradingProperties.Bithumb.OrderApiVersion.V2);
        BithumbOrderResponse v2res = order("v2-uuid", "done");
        when(v2Api.placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString())).thenReturn(v2res);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"));

        assertThat(res).isSameAs(v2res);
        verify(v2Api).placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString());
        verify(privateApi, never()).placeMarketBuyOrder(anyString(), any(), anyString());
        verify(privateApi, never()).placeMarketBuyOrder(anyString(), any());
    }

    @Test
    void v2Version_sell_routesToV2Api_notPrivateApi() {
        props.getBithumb().setOrderApiVersion(TradingProperties.Bithumb.OrderApiVersion.V2);
        BithumbOrderResponse v2res = order("v2-sell", "done");
        when(v2Api.placeMarketSellOrder(eq("KRW-ADA"), any(), anyString())).thenReturn(v2res);

        BithumbOrderResponse res = client.placeMarketSellOrder(new BigDecimal("5"));

        assertThat(res).isSameAs(v2res);
        verify(v2Api).placeMarketSellOrder(eq("KRW-ADA"), any(), anyString());
        verify(privateApi, never()).placeMarketSellOrder(anyString(), any(), anyString());
    }

    @Test
    void v1Version_buy_routesToPrivateApi_notV2Api() {
        // props 기본 V1 + clientOrderIdEnabled true(setUp)
        when(privateApi.placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString())).thenReturn(order("v1", "wait"));

        client.placeMarketBuyOrder(new BigDecimal("6000"));

        verify(privateApi).placeMarketBuyOrder(eq("KRW-ADA"), any(), anyString());
        verify(v2Api, never()).placeMarketBuyOrder(anyString(), any(), anyString());
    }

    @Test
    void paperMode_v2Version_doesNotCallEitherApi() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);
        props.getBithumb().setOrderApiVersion(TradingProperties.Bithumb.OrderApiVersion.V2);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"));

        assertThat(res).isNotNull(); // 시뮬레이션
        verify(v2Api, never()).placeMarketBuyOrder(anyString(), any(), anyString());
        verify(privateApi, never()).placeMarketBuyOrder(anyString(), any(), anyString());
    }

    @Test
    void paperMode_flagOn_doesNotCallPrivateApi() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"));

        assertThat(res).isNotNull(); // 시뮬레이션 체결
        verify(privateApi, never()).placeMarketBuyOrder(anyString(), any(), anyString());
        verify(privateApi, never()).placeMarketBuyOrder(anyString(), any());
        verify(privateApi, never()).getOrderByClientOrderId(anyString());
    }

    // §8-B: 선영속화를 위해 서비스가 cid 를 소유한다 — 주어진 cid 오버로드는 그 cid 를 전송·재조회에 그대로 사용.

    @Test
    void explicitCid_buy_v1FlagOn_usesGivenCidForOrderAndReconcile() {
        when(privateApi.placeMarketBuyOrder(eq("KRW-ADA"), any(), eq("cid-svc"))).thenReturn(null);
        when(privateApi.getOrderByClientOrderId("cid-svc")).thenReturn(null);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"), "cid-svc");

        assertThat(res).isNull();
        verify(privateApi).placeMarketBuyOrder(eq("KRW-ADA"), any(), eq("cid-svc"));
        verify(privateApi).getOrderByClientOrderId("cid-svc");
    }

    @Test
    void explicitCid_buy_v2_passesGivenCidToV2Api() {
        props.getBithumb().setOrderApiVersion(TradingProperties.Bithumb.OrderApiVersion.V2);
        BithumbOrderResponse v2res = order("v2-uuid", "done");
        when(v2Api.placeMarketBuyOrder(eq("KRW-ADA"), any(), eq("cid-svc"))).thenReturn(v2res);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"), "cid-svc");

        assertThat(res).isSameAs(v2res);
        verify(v2Api).placeMarketBuyOrder(eq("KRW-ADA"), any(), eq("cid-svc"));
    }

    @Test
    void explicitCid_buy_v1FlagOff_cidNotAttached_delegatesToTwoArg() {
        // v1 의 client_order_id 지원은 미검증 — 플래그 OFF 면 cid 를 거래소로 보내지 않는다(기존 동작 유지).
        props.getBithumb().setClientOrderIdEnabled(false);
        BithumbOrderResponse placed = order("uuid-5", "wait");
        when(privateApi.placeMarketBuyOrder("KRW-ADA", new BigDecimal("6000"))).thenReturn(placed);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"), "cid-svc");

        assertThat(res).isSameAs(placed);
        verify(privateApi).placeMarketBuyOrder("KRW-ADA", new BigDecimal("6000"));
        verify(privateApi, never()).placeMarketBuyOrder(anyString(), any(), anyString());
    }

    @Test
    void explicitCid_buy_paperMode_simulates_noRealOrder() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);

        BithumbOrderResponse res = client.placeMarketBuyOrder(new BigDecimal("6000"), "cid-svc");

        assertThat(res).isNotNull(); // 시뮬레이션 체결
        verify(privateApi, never()).placeMarketBuyOrder(anyString(), any(), anyString());
        verify(privateApi, never()).placeMarketBuyOrder(anyString(), any());
    }

    // §8-B: 틱 스윕이 쓰는 cid 재조회 — LIVE 에서만 실계정 조회(모드 게이트).

    @Test
    void getOrderByClientOrderId_live_delegatesToPrivateApi() {
        BithumbOrderResponse found = order("uuid-6", "done");
        when(privateApi.getOrderByClientOrderId("cid-x")).thenReturn(found);

        assertThat(client.getOrderByClientOrderId("cid-x")).isSameAs(found);
    }

    @Test
    void getOrderByClientOrderId_paperMode_returnsNullWithoutCall() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);

        assertThat(client.getOrderByClientOrderId("cid-x")).isNull();
        verify(privateApi, never()).getOrderByClientOrderId(anyString());
    }

    // §8-B: 선영속화 게이트 — cid 가 실제로 주문에 부착될 때만 true (v2 이거나 v1+플래그 ON).

    @Test
    void supportsClientOrderId_reflectsVersionAndFlag() {
        props.getBithumb().setClientOrderIdEnabled(false);
        assertThat(client.supportsClientOrderId()).isFalse(); // v1 + OFF (기본)

        props.getBithumb().setClientOrderIdEnabled(true);
        assertThat(client.supportsClientOrderId()).isTrue();  // v1 + ON

        props.getBithumb().setClientOrderIdEnabled(false);
        props.getBithumb().setOrderApiVersion(TradingProperties.Bithumb.OrderApiVersion.V2);
        assertThat(client.supportsClientOrderId()).isTrue();  // v2 는 공식 지원
    }

    @Test
    void newClientOrderId_matchesBithumbConstraint() {
        assertThat(client.newClientOrderId()).matches("^[A-Za-z0-9_-]{1,36}$");
    }

    @Test
    void newClientOrderId_carriesVersionPrefix_forTraceability() {
        // §6 호환성: 어떤 API 버전으로 생성된 주문인지 cid 만으로 추적 (t1-/t2-)
        assertThat(client.newClientOrderId()).startsWith("t1-"); // 기본 V1
        props.getBithumb().setOrderApiVersion(TradingProperties.Bithumb.OrderApiVersion.V2);
        assertThat(client.newClientOrderId()).startsWith("t2-");
    }

    // §8-B 매도 확장: 서비스가 cid 를 소유하는 매도 오버로드.

    @Test
    void explicitCid_sell_v1FlagOn_usesGivenCidForOrderAndReconcile() {
        when(privateApi.placeMarketSellOrder(eq("KRW-ADA"), any(), eq("cid-svc"))).thenReturn(null);
        when(privateApi.getOrderByClientOrderId("cid-svc")).thenReturn(null);

        BithumbOrderResponse res = client.placeMarketSellOrder(new BigDecimal("5"), "cid-svc");

        assertThat(res).isNull();
        verify(privateApi).placeMarketSellOrder(eq("KRW-ADA"), any(), eq("cid-svc"));
        verify(privateApi).getOrderByClientOrderId("cid-svc");
    }

    @Test
    void explicitCid_sell_v2_passesGivenCidToV2Api() {
        props.getBithumb().setOrderApiVersion(TradingProperties.Bithumb.OrderApiVersion.V2);
        BithumbOrderResponse v2res = order("v2-sell", "done");
        when(v2Api.placeMarketSellOrder(eq("KRW-ADA"), any(), eq("cid-svc"))).thenReturn(v2res);

        BithumbOrderResponse res = client.placeMarketSellOrder(new BigDecimal("5"), "cid-svc");

        assertThat(res).isSameAs(v2res);
        verify(v2Api).placeMarketSellOrder(eq("KRW-ADA"), any(), eq("cid-svc"));
    }

    // Phase 0b 잔여: 지정가 주문에도 cid 부착 (v1, 플래그 ON 일 때) + null 응답 시 재조회.

    @Test
    void limitBuy_flagOn_attachesCid_andReconcilesOnNull() {
        when(privateApi.placeLimitOrder(eq("KRW-ADA"), eq("bid"), any(), any(), anyString())).thenReturn(null);
        when(privateApi.getOrderByClientOrderId(anyString())).thenReturn(null);

        BithumbOrderResponse res = client.placeLimitBuyOrder(new BigDecimal("5"), new BigDecimal("1000"));

        assertThat(res).isNull();
        verify(privateApi).placeLimitOrder(eq("KRW-ADA"), eq("bid"), any(), any(), anyString());
        verify(privateApi, never()).placeLimitOrder(anyString(), anyString(), any(), any());
        verify(privateApi).getOrderByClientOrderId(anyString());
    }

    @Test
    void limitSell_flagOff_delegatesToFourArg_noCid() {
        props.getBithumb().setClientOrderIdEnabled(false);
        BithumbOrderResponse placed = order("uuid-l1", "wait");
        when(privateApi.placeLimitOrder(eq("KRW-ADA"), eq("ask"), any(), any())).thenReturn(placed);

        BithumbOrderResponse res = client.placeLimitSellOrder(new BigDecimal("5"), new BigDecimal("1200"));

        assertThat(res).isSameAs(placed);
        verify(privateApi).placeLimitOrder(eq("KRW-ADA"), eq("ask"), any(), any());
        verify(privateApi, never()).placeLimitOrder(anyString(), anyString(), any(), any(), anyString());
    }

    // Phase 1 잔여: 취소 버전 라우팅 — v1→privateApi, v2→v2Api, LIVE 아니면 미전송(§8-A).

    @Test
    void v1Version_cancel_routesToPrivateApi() {
        BithumbOrderResponse cancelled = order("uuid-c1", "cancel");
        when(privateApi.cancelOrder("uuid-c1")).thenReturn(cancelled);

        assertThat(client.cancelOrder("uuid-c1")).isSameAs(cancelled);
        verify(v2Api, never()).cancelOrder(anyString());
    }

    @Test
    void v2Version_cancel_routesToV2Api_notPrivateApi() {
        props.getBithumb().setOrderApiVersion(TradingProperties.Bithumb.OrderApiVersion.V2);
        BithumbOrderResponse cancelled = order("uuid-c2", "cancel");
        when(v2Api.cancelOrder("uuid-c2")).thenReturn(cancelled);

        assertThat(client.cancelOrder("uuid-c2")).isSameAs(cancelled);
        verify(v2Api).cancelOrder("uuid-c2");
        verify(privateApi, never()).cancelOrder(anyString());
    }

    @Test
    void paperMode_v2Version_cancel_doesNotCallEitherApi() {
        props.getBot().setMode(TradingProperties.Bot.Mode.PAPER);
        props.getBithumb().setOrderApiVersion(TradingProperties.Bithumb.OrderApiVersion.V2);

        assertThat(client.cancelOrder("uuid-c3")).isNull();
        verify(v2Api, never()).cancelOrder(anyString());
        verify(privateApi, never()).cancelOrder(anyString());
    }
}
