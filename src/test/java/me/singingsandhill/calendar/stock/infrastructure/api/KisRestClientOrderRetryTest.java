package me.singingsandhill.calendar.stock.infrastructure.api;

import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import me.singingsandhill.calendar.stock.application.observability.StockBotMetrics;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 주문/조회 재시도 정책 회귀 테스트 (2026-07-24 리뷰 §3-③).
 *
 * 주문 POST 는 비멱등이다 — KIS 가 주문을 접수했는데 응답만 유실(타임아웃/5xx)되면
 * 재시도가 동일 시장가 주문을 중복 전송한다. 기대: 주문은 무재시도(실패 즉시 반환),
 * 시세 조회(GET)는 기존 재시도 유지.
 */
class KisRestClientOrderRetryTest {

    private MockWebServer server;
    private KisRestClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();

        StockProperties props = new StockProperties();
        props.getKis().setBaseUrl(server.url("/").toString());

        KisAuthService authService = mock(KisAuthService.class);
        when(authService.isConfigured()).thenReturn(true);
        when(authService.buildAuthHeaders(anyString())).thenReturn(new HashMap<>());
        when(authService.getAccountNumber()).thenReturn("12345678");
        when(authService.getAccountProductCode()).thenReturn("01");
        when(authService.generateHashkey(any())).thenReturn("hashkey");

        @SuppressWarnings("unchecked")
        ObjectProvider<StockBotMetrics> metrics = mock(ObjectProvider.class);

        client = new KisRestClient(WebClient.builder(), authService, props, metrics);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    private static MockResponse json(String body) {
        return new MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(body);
    }

    @Test
    void orderPost_doesNotRetryOn5xx() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{}"));
        server.enqueue(json("{\"rt_cd\":\"0\",\"msg_cd\":\"0\",\"msg1\":\"ok\","
            + "\"output\":{\"ODNO\":\"0000117057\",\"ORD_TMD\":\"090001\"}}"));

        client.placeBuyOrder("005930", 10, BigDecimal.ZERO, true);

        // 비멱등 주문은 5xx 여도 재전송하지 않는다 — 요청은 정확히 1회
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void quoteGet_stillRetriesOn5xx() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("{}"));
        server.enqueue(json("{\"rt_cd\":\"0\",\"output\":{\"stck_prpr\":\"70000\","
            + "\"stck_oprc\":\"69500\",\"stck_hgpr\":\"70500\",\"stck_lwpr\":\"69000\","
            + "\"stck_sdpr\":\"69000\",\"cttr\":\"105.5\"}}"));

        assertThat(client.getQuote("005930")).isNotNull();
        // 멱등 조회는 기존 재시도 유지 — 500 후 1회 재시도로 총 2회
        assertThat(server.getRequestCount()).isEqualTo(2);
    }
}
