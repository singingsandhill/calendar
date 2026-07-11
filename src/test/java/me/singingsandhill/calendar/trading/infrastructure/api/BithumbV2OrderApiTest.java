package me.singingsandhill.calendar.trading.infrastructure.api;

import me.singingsandhill.calendar.trading.infrastructure.api.auth.BithumbJwtGenerator;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 1: BithumbV2OrderApi 의 HTTP 계약을 MockWebServer 로 검증한다.
 * - 생성 요청 형식(order_type/side/client_order_id) + v2 응답 정규화(GET /v1/order 재조회로 trades 확보).
 * - 응답 유실(연결 종료) 시 재전송 금지 + client_order_id 재조회로 접수 여부 확인(P0-2 핵심 경로).
 */
class BithumbV2OrderApiTest {

    private static final String MARKET = "KRW-ADA";

    private MockWebServer server;
    private BithumbPrivateApi privateApi;
    private BithumbJwtGenerator jwt;
    private BithumbV2OrderApi api;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        TradingProperties props = new TradingProperties();
        props.getBithumb().setBaseUrl(server.url("/").toString());

        privateApi = mock(BithumbPrivateApi.class);
        jwt = mock(BithumbJwtGenerator.class);
        when(jwt.isConfigured()).thenReturn(true);
        when(jwt.generateAuthorizationHeader(any())).thenReturn("Bearer test-token");

        api = new BithumbV2OrderApi(props, WebClient.builder(), jwt, privateApi);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private BithumbOrderResponse filledOrder(String uuid) {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "t1", "1000", "6", "6000", "bid", "2026-07-06T10:00:00");
        return new BithumbOrderResponse(uuid, "bid", "price", "1000", "done", MARKET,
                null, "6", "0", "0", "0", "15", "0", "6", 1, List.of(t));
    }

    @Test
    void marketBuy_createSucceeds_normalizesViaGetOrder_andSendsCorrectRequest() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"order_id\":\"oid-1\",\"client_order_id\":\"t-abc\",\"market\":\"KRW-ADA\","
                        + "\"side\":\"bid\",\"order_type\":\"price\",\"created_at\":\"2026-07-06T10:00:00\"}"));
        // v2 생성 응답엔 trades 가 없으므로 order_id 로 재조회해 체결 정보를 채운다
        BithumbOrderResponse full = filledOrder("oid-1");
        when(privateApi.getOrder("oid-1")).thenReturn(full);

        BithumbOrderResponse res = api.placeMarketBuyOrder(MARKET, new BigDecimal("6000"), "t-abc");

        // 정규화 결과 = 재조회로 채운 완전한 주문(trades 포함)
        assertThat(res).isSameAs(full);

        // 어댑터가 실제로 보낸 요청 검증
        RecordedRequest req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("POST");
        assertThat(req.getPath()).isEqualTo("/v2/orders");
        String body = req.getBody().readUtf8();
        assertThat(body).contains("\"order_type\":\"price\"");
        assertThat(body).contains("\"side\":\"bid\"");
        assertThat(body).contains("\"client_order_id\":\"t-abc\"");
        assertThat(body).contains("\"price\":\"6000\"");
    }

    @Test
    void marketBuy_responseLost_doesNotResend_reconcilesByClientOrderId() {
        // 서버가 요청은 받고 응답 없이 연결을 끊음 = "주문은 접수됐는데 응답만 유실"
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
        BithumbOrderResponse recovered = filledOrder("oid-2");
        when(privateApi.getOrderByClientOrderId("t-abc")).thenReturn(recovered);

        BithumbOrderResponse res = api.placeMarketBuyOrder(MARKET, new BigDecimal("6000"), "t-abc");

        // 재전송이 아니라 client_order_id 재조회로 복구
        assertThat(res).isSameAs(recovered);
        verify(privateApi).getOrderByClientOrderId("t-abc");
        verify(privateApi, never()).getOrder(anyString());
    }

    @Test
    void marketBuy_responseLost_reconcileEmpty_returnsNull() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
        when(privateApi.getOrderByClientOrderId("t-abc")).thenReturn(null);

        BithumbOrderResponse res = api.placeMarketBuyOrder(MARKET, new BigDecimal("6000"), "t-abc");

        assertThat(res).isNull();
    }

    // §8-E: 중복 client_order_id 에러는 실패가 아니라 "주문 존재 가능" — 재전송하지 않고 즉시 재조회.

    @Test
    void marketBuy_duplicateCidError_reconcilesByCid_noResend() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"name\":\"duplicate_client_order_id\","
                        + "\"message\":\"client_order_id already exists\"}}"));
        BithumbOrderResponse existing = filledOrder("oid-dup");
        when(privateApi.getOrderByClientOrderId("t-abc")).thenReturn(existing);

        BithumbOrderResponse res = api.placeMarketBuyOrder(MARKET, new BigDecimal("6000"), "t-abc");

        // 기존 주문 복구 — 이중 체결 방지
        assertThat(res).isSameAs(existing);
        assertThat(server.getRequestCount()).isEqualTo(1); // 재전송 없음
        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/v2/orders");
    }

    // 정규화 재조회 백오프: 접수 확인 후 GET /v1/order 가 일시 실패하면 최대 3회 백오프 재시도.

    @Test
    void normalize_requeryFailsTwice_thenSucceeds_returnsFullOrder() {
        api.setRequeryBackoffBaseMillis(1); // 테스트 속도
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"order_id\":\"oid-3\",\"client_order_id\":\"t-abc\",\"market\":\"KRW-ADA\","
                        + "\"side\":\"bid\",\"order_type\":\"price\",\"created_at\":\"2026-07-08T10:00:00\"}"));
        BithumbOrderResponse full = filledOrder("oid-3");
        when(privateApi.getOrder("oid-3")).thenReturn(null, null, full);

        BithumbOrderResponse res = api.placeMarketBuyOrder(MARKET, new BigDecimal("6000"), "t-abc");

        assertThat(res).isSameAs(full);
        verify(privateApi, org.mockito.Mockito.times(3)).getOrder("oid-3");
        verify(privateApi, never()).getOrderByClientOrderId(anyString());
    }

    @Test
    void normalize_allRequeriesFail_fallsBackToCid_thenUnknownPartial() {
        api.setRequeryBackoffBaseMillis(1);
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"order_id\":\"oid-4\",\"client_order_id\":\"t-abc\",\"market\":\"KRW-ADA\","
                        + "\"side\":\"bid\",\"order_type\":\"price\",\"created_at\":\"2026-07-08T10:00:00\"}"));
        when(privateApi.getOrder("oid-4")).thenReturn(null);
        when(privateApi.getOrderByClientOrderId("t-abc")).thenReturn(null);

        BithumbOrderResponse res = api.placeMarketBuyOrder(MARKET, new BigDecimal("6000"), "t-abc");

        // 접수는 확인됐으므로 null 이 아니라 state=UNKNOWN 부분 응답 (스윕이 수습)
        assertThat(res).isNotNull();
        assertThat(res.state()).isEqualTo("UNKNOWN");
        assertThat(res.uuid()).isEqualTo("oid-4");
        verify(privateApi, org.mockito.Mockito.times(3)).getOrder("oid-4");
    }

    // Phase 1 잔여: v2 취소 (DELETE /v2/order) — 응답 후 GET /v1/order 재조회로 정규화(생성과 동일 패턴).

    @Test
    void cancel_sendsDeleteV2Order_andNormalizesViaGetOrder() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"order_id\":\"oid-5\",\"client_order_id\":\"t-abc\",\"market\":\"KRW-ADA\","
                        + "\"side\":\"bid\",\"order_type\":\"price\",\"created_at\":\"2026-07-08T10:00:00\"}"));
        BithumbOrderResponse cancelled = new BithumbOrderResponse("oid-5", "bid", "price", null, "cancel",
                MARKET, null, null, null, null, null, null, null, null, null, null);
        when(privateApi.getOrder("oid-5")).thenReturn(cancelled);

        BithumbOrderResponse res = api.cancelOrder("oid-5");

        assertThat(res).isSameAs(cancelled);
        RecordedRequest req = server.takeRequest();
        assertThat(req.getMethod()).isEqualTo("DELETE");
        assertThat(req.getPath()).isEqualTo("/v2/order?order_id=oid-5");
    }

    @Test
    void cancel_httpError_returnsNull() {
        server.enqueue(new MockResponse().setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"name\":\"order_not_found\"}}"));

        BithumbOrderResponse res = api.cancelOrder("oid-6");

        assertThat(res).isNull();
        verify(privateApi, never()).getOrder(anyString());
    }

    // Phase 1 (4): 422(주문 처리 중) 취소는 짧은 백오프 후 1회 재시도.

    @Test
    void cancel_422Processing_retriesOnce_thenSucceeds() {
        api.setRequeryBackoffBaseMillis(1);
        server.enqueue(new MockResponse().setResponseCode(422)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"name\":\"order_processing\"}}"));
        server.enqueue(new MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"order_id\":\"oid-7\",\"market\":\"KRW-ADA\",\"side\":\"bid\","
                        + "\"order_type\":\"price\",\"created_at\":\"2026-07-08T10:00:00\"}"));
        BithumbOrderResponse cancelled = new BithumbOrderResponse("oid-7", "bid", "price", null, "cancel",
                MARKET, null, null, null, null, null, null, null, null, null, null);
        when(privateApi.getOrder("oid-7")).thenReturn(cancelled);

        BithumbOrderResponse res = api.cancelOrder("oid-7");

        assertThat(res).isSameAs(cancelled);
        assertThat(server.getRequestCount()).isEqualTo(2); // 원 요청 + 재시도 1회
    }

    @Test
    void cancel_422Twice_givesUp_returnsNull() {
        api.setRequeryBackoffBaseMillis(1);
        server.enqueue(new MockResponse().setResponseCode(422)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"name\":\"order_processing\"}}"));
        server.enqueue(new MockResponse().setResponseCode(422)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"name\":\"order_processing\"}}"));

        BithumbOrderResponse res = api.cancelOrder("oid-8");

        assertThat(res).isNull();
        assertThat(server.getRequestCount()).isEqualTo(2); // 재시도는 1회뿐
    }
}
