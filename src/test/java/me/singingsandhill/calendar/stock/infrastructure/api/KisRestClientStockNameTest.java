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

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 종목명 데이터 소스 회귀 테스트 (2026-08-31 메일 종목명 칸에 종목코드 인쇄,
 * ADR stock/infrastructure/0008).
 *
 * 스크리닝이 쓰는 시세(inquire-price)·체결(inquire-ccnl)·호가 TR 에는 종목명 필드가 없다.
 * 종목명은 주식기본조회(CTPF1002R, search-stock-info)의 {@code prdt_abrv_name}(약칭)이
 * 원천이고, 없으면 {@code prdt_name}. 실패/필드 부재는 null — 호출측이 종목코드로 대체한다.
 * 이 TR 의 HTTP 경로(엔드포인트·tr_id·파라미터)를 검증하는 테스트는 이 파일이 유일하다.
 */
class KisRestClientStockNameTest {

    /** 약칭과 정식명을 다르게 두어 어느 필드를 썼는지 판별한다. */
    private static final String INFO_BODY = "{\"rt_cd\":\"0\",\"msg_cd\":\"MCA00000\",\"msg1\":\"정상처리 되었습니다!\","
        + "\"output\":{\"pdno\":\"000000005930\",\"prdt_type_cd\":\"300\",\"std_pdno\":\"KR7005930003\","
        + "\"prdt_name\":\"삼성전자보통주\",\"prdt_abrv_name\":\"삼성전자\",\"prdt_eng_name\":\"SamsungElectronics\"}}";

    private MockWebServer server;
    private KisAuthService authService;
    private KoreaInvestmentApiClient apiClient;
    private final AtomicInteger infoRequests = new AtomicInteger();
    private volatile String lastInfoPath;
    private volatile int infoFailuresBeforeSuccess = 0;
    private volatile String infoBody = INFO_BODY;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String path = request.getPath() == null ? "" : request.getPath();
                if (path.startsWith("/uapi/domestic-stock/v1/quotations/search-stock-info")) {
                    lastInfoPath = path;
                    if (infoRequests.incrementAndGet() <= infoFailuresBeforeSuccess) {
                        return new MockResponse().setResponseCode(500).setBody("{}");
                    }
                    return json(infoBody);
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

        KisRestClient restClient = new KisRestClient(WebClient.builder(), authService, props, metrics);
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
    void stockName_comesFromSearchStockInfoAbbreviatedName() {
        String name = apiClient.getStockName("005930");

        // 약칭(prdt_abrv_name) 우선 — 정식명(prdt_name)이 아니다
        assertThat(name).isEqualTo("삼성전자");
        assertThat(infoRequests.get()).isEqualTo(1);
        // KIS 는 tr_id 로 TR 을 식별한다 — 경로가 맞아도 tr_id 가 틀리면 실API 는 실패
        verify(authService).buildAuthHeaders("CTPF1002R");
        // PRDT_TYPE_CD 300 = 주식/ETF/ETN/ELW
        assertThat(lastInfoPath).contains("PDNO=005930").contains("PRDT_TYPE_CD=300");
    }

    @Test
    void stockName_fallsBackToPrdtNameWhenAbbreviationBlank() {
        infoBody = "{\"rt_cd\":\"0\",\"output\":{\"pdno\":\"000000005930\","
            + "\"prdt_name\":\"삼성전자보통주\",\"prdt_abrv_name\":\"\"}}";

        assertThat(apiClient.getStockName("005930")).isEqualTo("삼성전자보통주");
    }

    @Test
    void stockName_retriesOn5xxLikeOtherGets() {
        infoFailuresBeforeSuccess = 1;

        String name = apiClient.getStockName("005930");

        // 멱등 조회는 기존 재시도 정책 유지 — 500 후 재시도로 총 2회
        assertThat(name).isEqualTo("삼성전자");
        assertThat(infoRequests.get()).isEqualTo(2);
    }

    @Test
    void stockName_returnsNullWhenRtCdNotZero() {
        infoBody = "{\"rt_cd\":\"1\",\"msg_cd\":\"EGW00123\",\"msg1\":\"기간이 만료된 token 입니다.\",\"output\":{}}";

        assertThat(apiClient.getStockName("005930")).isNull();
    }

    @Test
    void stockName_returnsNullWhenNameFieldsMissing() {
        // 스펙 미검증 필드 매핑 사고(ADR 0007 의 cttr) 경로 — 값이 없으면 null, 호출측이 코드로 대체
        infoBody = "{\"rt_cd\":\"0\",\"output\":{\"pdno\":\"000000005930\",\"std_pdno\":\"KR7005930003\"}}";

        assertThat(apiClient.getStockName("005930")).isNull();
    }

    @Test
    void stockName_returnsNullAfterRetryExhaustionOn5xx() {
        infoFailuresBeforeSuccess = Integer.MAX_VALUE;

        assertThat(apiClient.getStockName("005930")).isNull();
        // 백오프 총량이 전체 timeout(10s) 안에 들어오지 않을 수 있어 정확 횟수는 단언하지 않는다
        assertThat(infoRequests.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void stockName_skipsWhenNotConfigured() {
        when(authService.isConfigured()).thenReturn(false);

        assertThat(apiClient.getStockName("005930")).isNull();
        assertThat(infoRequests.get()).isZero();
    }
}
