package me.singingsandhill.calendar.stock.infrastructure.api;

import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import me.singingsandhill.calendar.stock.application.observability.StockBotMetrics;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 주문·체결조회 TR ID 회귀 테스트 (2026-08-31 KIS 스펙 대조).
 *
 * 공식 문서(주식주문(현금)[v1_국내주식-001], 주식일별주문체결조회) tr_id 항목:
 * "구TR은 사전고지 없이 막힐 수 있으므로 반드시 신TR로 변경이용 부탁드립니다."
 *   매수 (구)TTTC0802U → (신)TTTC0012U / 매도 (구)TTTC0801U → (신)TTTC0011U (모의 VTTC 동일)
 *   체결조회 3개월이내 (구)TTTC8001R → (신)TTTC0081R (모의 VTTC 동일)
 * 구TR 차단 시 LIVE 매도(손절·최종청산)가 실패해 포지션이 무보호로 남는 실자금 경로 —
 * KIS 는 tr_id 로 TR 을 식별하므로 경로가 맞아도 tr_id 가 틀리면 실API 는 실패한다.
 *
 * 신TR 주문 바디의 거래소ID구분(EXCG_ID_DVSN_CD)은 KRX 로 명시한다 — 봇의 모든 시세가
 * KRX 전용(FID_COND_MRKT_DIV_CODE=J)이라, 기본값 변경으로 주문만 SOR/NXT 로 새는 것을 차단.
 */
class KisRestClientTrIdTest {

    private static final String ORDER_BODY = "{\"rt_cd\":\"0\",\"msg_cd\":\"0\",\"msg1\":\"ok\","
        + "\"output\":{\"ODNO\":\"0000117057\",\"ORD_TMD\":\"090001\"}}";

    private static final String CCLD_BODY = "{\"rt_cd\":\"0\",\"msg_cd\":\"0\",\"msg1\":\"ok\","
        + "\"output1\":[]}";

    private MockWebServer server;
    private KisAuthService authService;
    private KisRestClient client;
    private StockProperties props;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath();
                if (path.startsWith("/uapi/domestic-stock/v1/trading/order-cash")) {
                    return json(ORDER_BODY);
                }
                if (path.startsWith("/uapi/domestic-stock/v1/trading/inquire-daily-ccld")) {
                    return json(CCLD_BODY);
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        server.start();

        props = new StockProperties();
        props.getKis().setBaseUrl(server.url("/").toString());

        authService = mock(KisAuthService.class);
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

    private String takeRequestBody() throws Exception {
        RecordedRequest recorded = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        return recorded.getBody().readUtf8();
    }

    @Test
    void buyOrder_usesNewTrId_andPinsKrxExchange() throws Exception {
        client.placeBuyOrder("005930", 10, BigDecimal.ZERO, true);

        verify(authService).buildAuthHeaders("TTTC0012U");
        // 시세(J=KRX)와 주문 라우팅의 결합을 바디에 고정
        assertThat(takeRequestBody()).contains("\"EXCG_ID_DVSN_CD\":\"KRX\"");
    }

    @Test
    void sellOrder_usesNewTrId_andPinsKrxExchange() throws Exception {
        client.placeSellOrder("005930", 10, new BigDecimal("70000"), false);

        verify(authService).buildAuthHeaders("TTTC0011U");
        assertThat(takeRequestBody()).contains("\"EXCG_ID_DVSN_CD\":\"KRX\"");
    }

    @Test
    void orderHistory_usesNewTrId() {
        client.getOrderHistory(LocalDate.of(2026, 8, 31));

        verify(authService).buildAuthHeaders("TTTC0081R");
    }

    @Test
    void paperTrading_alsoUsesNewTrId() {
        // 분기식의 모의투자 쪽도 신TR — production 토글이 구TR 로 회귀하지 않는지 가드
        props.getKis().setProduction(false);

        client.placeBuyOrder("005930", 10, BigDecimal.ZERO, true);

        verify(authService).buildAuthHeaders("VTTC0012U");
    }
}
