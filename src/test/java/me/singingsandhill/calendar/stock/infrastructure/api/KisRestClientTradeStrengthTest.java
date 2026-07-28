package me.singingsandhill.calendar.stock.infrastructure.api;

import me.singingsandhill.calendar.stock.application.observability.StockBotMetrics;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
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
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 체결강도 데이터 소스 회귀 테스트 (2026-07-27 스크리닝 전멸, ADR stock/infrastructure/0007).
 *
 * KIS 주식현재가 시세(FHKST01010100, inquire-price) 응답에는 체결강도 필드가 없다 —
 * {@code cttr} 도, 과거 폴백이던 {@code seln_cntg_smtn}/{@code shnu_cntg_smtn} 도
 * 공식 스펙(koreainvestment/open-trading-api 샘플, 82필드)에 존재하지 않는다.
 * 체결강도는 주식현재가 체결(FHKST01010300, inquire-ccnl)의 {@code tday_rltv} 가 원천이다.
 * 여기서는 시세 응답을 실스펙대로(체결강도 필드 없이) 스텁하고, 조회가 올바른
 * 엔드포인트·tr_id·종목 파라미터로 나가는 것까지 고정한다 — 이 HTTP 경로를 검증하는
 * 테스트는 이 파일이 유일하다(상위 테스트는 getTradeStrength 자체를 목킹).
 */
class KisRestClientTradeStrengthTest {

    /** 실스펙 반영: 체결강도 관련 필드가 없는 inquire-price 응답. */
    private static final String QUOTE_BODY = "{\"rt_cd\":\"0\",\"output\":{"
        + "\"stck_prpr\":\"10400\",\"stck_oprc\":\"10400\",\"stck_hgpr\":\"10500\","
        + "\"stck_lwpr\":\"10300\",\"stck_sdpr\":\"10000\",\"acml_vol\":\"1000000\","
        + "\"acml_tr_pbmn\":\"10000000000\",\"hts_avls\":\"1000\"}}";

    private static final String CCNL_BODY = "{\"rt_cd\":\"0\",\"output\":["
        + "{\"stck_cntg_hour\":\"092010\",\"stck_prpr\":\"10400\",\"tday_rltv\":\"142.35\"},"
        + "{\"stck_cntg_hour\":\"092005\",\"stck_prpr\":\"10390\",\"tday_rltv\":\"120.00\"}]}";

    private MockWebServer server;
    private KisAuthService authService;
    private KisRestClient restClient;
    private KoreaInvestmentApiClient apiClient;
    private final AtomicInteger ccnlRequests = new AtomicInteger();
    private final AtomicInteger priceRequests = new AtomicInteger();
    private volatile String lastCcnlPath;
    private volatile int ccnlFailuresBeforeSuccess = 0;
    private volatile String ccnlBody = CCNL_BODY;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath();
                if (path.startsWith("/uapi/domestic-stock/v1/quotations/inquire-price")) {
                    priceRequests.incrementAndGet();
                    return json(QUOTE_BODY);
                }
                if (path.startsWith("/uapi/domestic-stock/v1/quotations/inquire-ccnl")) {
                    lastCcnlPath = path;
                    if (ccnlRequests.incrementAndGet() <= ccnlFailuresBeforeSuccess) {
                        return new MockResponse().setResponseCode(500).setBody("{}");
                    }
                    return json(ccnlBody);
                }
                return new MockResponse().setResponseCode(404);
            }
        });
        server.start();

        StockProperties props = new StockProperties();
        props.getKis().setBaseUrl(server.url("/").toString());

        authService = mock(KisAuthService.class);
        when(authService.isConfigured()).thenReturn(true);
        when(authService.buildAuthHeaders(anyString())).thenReturn(new HashMap<>());

        @SuppressWarnings("unchecked")
        ObjectProvider<StockBotMetrics> metrics = mock(ObjectProvider.class);

        restClient = new KisRestClient(WebClient.builder(), authService, props, metrics);
        apiClient = new KoreaInvestmentApiClient(authService, restClient, props);
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
    void tradeStrength_comesFromInquireCcnlLatestRow() {
        BigDecimal strength = apiClient.getTradeStrength("005930");

        // 최신(첫) 체결 행의 tday_rltv — 시세(inquire-price)에서는 얻을 수 없는 값
        assertThat(strength).isEqualByComparingTo(new BigDecimal("142.35"));
        assertThat(ccnlRequests.get()).isEqualTo(1);
        // 시세 엔드포인트로는 나가지 않는다 — 소스 오선택 회귀 가드
        assertThat(priceRequests.get()).isZero();
        // KIS 는 tr_id 로 TR 을 식별한다 — 경로가 맞아도 tr_id 가 틀리면 실API 는 실패
        verify(authService).buildAuthHeaders("FHKST01010300");
        assertThat(lastCcnlPath).contains("FID_INPUT_ISCD=005930").contains("FID_COND_MRKT_DIV_CODE=J");
    }

    @Test
    void tradeStrength_retriesOn5xxLikeOtherGets() {
        ccnlFailuresBeforeSuccess = 1;

        BigDecimal strength = apiClient.getTradeStrength("005930");

        // 멱등 조회는 기존 재시도 정책 유지 — 500 후 재시도로 총 2회
        assertThat(strength).isEqualByComparingTo(new BigDecimal("142.35"));
        assertThat(ccnlRequests.get()).isEqualTo(2);
    }

    @Test
    void tradeStrength_returnsNullWhenOutputEmpty() {
        ccnlBody = "{\"rt_cd\":\"0\",\"output\":[]}";

        // 미집계는 0 이 아니라 null — 호출측(스크리닝/진입검증)이 데이터 부족으로 처리
        assertThat(apiClient.getTradeStrength("005930")).isNull();
    }

    @Test
    void tradeStrength_returnsNullWhenFieldMissing() {
        ccnlBody = "{\"rt_cd\":\"0\",\"output\":[{\"stck_cntg_hour\":\"092010\",\"stck_prpr\":\"10400\"}]}";

        assertThat(apiClient.getTradeStrength("005930")).isNull();
    }
}
