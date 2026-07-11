package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeStatus;
import me.singingsandhill.calendar.trading.domain.trade.TradeType;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbPrivateApi;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbPublicApi;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbV2OrderApi;
import me.singingsandhill.calendar.trading.infrastructure.api.auth.BithumbJwtGenerator;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderbookResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * §8-B E2E (§6.5 마지막 항목): "v2 생성 응답 유실 → 선영속화 SUBMITTED 유지 → 틱 스윕이 나중에
 * 체결 발견 → Trade DONE + Position(SL/TP) 생성" 전 경로를 실제 컴포넌트 결선으로 검증한다.
 * BithumbV2OrderApi(HTTP, MockWebServer) + BithumbApiClient(라우팅) + TradingBotService(선영속화·스윕) 실물,
 * BithumbPrivateApi(재조회)·저장소는 목.
 */
class TradingV2LostResponseSweepTest {

    private static final String MARKET = "KRW-ADA";

    private MockWebServer server;
    private BithumbPrivateApi privateApi;
    private BithumbPublicApi publicApi;
    private TradeRepository tradeRepo;
    private PositionRepository posRepo;
    private RiskManagementService risk;
    private TradingBotService svc;
    private final List<Trade> submittedInDb = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        TradingProperties props = new TradingProperties();
        props.getBithumb().setBaseUrl(server.url("/").toString());
        props.getBithumb().setOrderApiVersion(TradingProperties.Bithumb.OrderApiVersion.V2);
        props.getBot().setMarket(MARKET);
        props.getBot().setMode(TradingProperties.Bot.Mode.LIVE);

        privateApi = mock(BithumbPrivateApi.class);
        publicApi = mock(BithumbPublicApi.class);
        BithumbJwtGenerator jwt = mock(BithumbJwtGenerator.class);
        when(jwt.isConfigured()).thenReturn(true);
        when(jwt.generateAuthorizationHeader(any())).thenReturn("Bearer test-token");

        BithumbV2OrderApi v2Api = new BithumbV2OrderApi(props, WebClient.builder(), jwt, privateApi);
        BithumbApiClient api = new BithumbApiClient(publicApi, privateApi, v2Api, props);

        tradeRepo = mock(TradeRepository.class);
        posRepo = mock(PositionRepository.class);
        risk = mock(RiskManagementService.class);
        TradingCircuitBreaker breaker = mock(TradingCircuitBreaker.class);
        AccountSnapshotRepository snapRepo = mock(AccountSnapshotRepository.class);
        IndicatorService indicators = mock(IndicatorService.class);
        RebalanceService rebalance = mock(RebalanceService.class);
        PlatformTransactionManager txm = mock(PlatformTransactionManager.class);

        svc = new TradingBotService(null, null, indicators, risk, rebalance, api, tradeRepo, posRepo,
                props, mock(TradingEventService.class), breaker, snapRepo, txm);

        // 가드 통과 셋업
        when(breaker.isEntryBlocked(any(), any())).thenReturn(false);
        when(snapRepo.findFirstByMarketAndDateRange(any(), any(), any())).thenReturn(Optional.empty());
        when(posRepo.findByMarketAndStatusAndClosedAtBetween(any(), any(), any(), any())).thenReturn(List.of());
        when(privateApi.getAccount("KRW")).thenReturn(acct("KRW", "1000000"));
        when(privateApi.getAccount("ADA")).thenReturn(acct("ADA", "0"));
        when(publicApi.getOrderbook(MARKET, true)).thenReturn(new BithumbOrderbookResponse(
                MARKET, 0L, 0.0, 0.0,
                List.of(new BithumbOrderbookResponse.OrderbookUnit(1001.0, 999.0, 1.0, 1.0))));
        when(posRepo.findByMarketAndStatus(any(), any())).thenReturn(List.of());
        when(indicators.calculateATRPercent(any())).thenReturn(null);
        when(risk.calculateStopLossPrice(any())).thenReturn(new BigDecimal("985"));
        when(risk.calculateTakeProfitPrice(any())).thenReturn(new BigDecimal("1030"));
        when(tradeRepo.findByStatus(TradeStatus.SUBMITTED)).thenAnswer(inv -> new ArrayList<>(submittedInDb));
        when(posRepo.save(any())).thenAnswer(inv -> {
            Position p = inv.getArgument(0);
            p.setId(77L);
            return p;
        });
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private BithumbAccountResponse acct(String currency, String balance) {
        return new BithumbAccountResponse(currency, balance, "0", null, null, "KRW");
    }

    private BithumbOrderResponse filledOrder(String uuid) {
        BithumbOrderResponse.TradeDetail t = new BithumbOrderResponse.TradeDetail(
                MARKET, "t1", "1000", "248", "248000", "bid", "2026-07-08T10:00:00");
        return new BithumbOrderResponse(uuid, "bid", "price", "1000", "done", MARKET,
                null, "248", "0", "0", "0", "621.875", "0", "248", 1, List.of(t));
    }

    @Test
    void lostCreateResponse_submittedKept_thenSweepConfirmsFill_andOpensPosition() throws InterruptedException {
        // ── 1단계: v2 생성 요청은 접수됐지만 응답이 유실됨 (연결 종료) ──
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST));
        // 즉시 재조회(cid)도 아직 못 찾음 → 파사드는 null 반환
        when(privateApi.getOrderByClientOrderId(anyString())).thenReturn(null);

        Signal signal = mock(Signal.class);
        when(signal.getTotalScore()).thenReturn(50);
        svc.executeBuy(MARKET, signal);

        // 선영속화된 Trade 가 SUBMITTED 로 남고 Position 은 없다
        ArgumentCaptor<Trade> savedTrade = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo).save(savedTrade.capture());
        Trade submitted = savedTrade.getValue();
        assertThat(submitted.getStatus()).isEqualTo(TradeStatus.SUBMITTED);
        assertThat(submitted.getClientOrderId()).isNotBlank();
        verify(posRepo, never()).save(any());

        // 재전송 없음: /v2/orders 요청은 정확히 1회, cid 가 요청 본문에 부착됨
        assertThat(server.getRequestCount()).isEqualTo(1);
        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/v2/orders");
        assertThat(req.getBody().readUtf8()).contains("\"client_order_id\":\"" + submitted.getClientOrderId() + "\"");

        // ── 2단계: 다음 틱 스윕 — 거래소가 이제 체결 완료를 반환 ──
        String cid = submitted.getClientOrderId();
        // 스윕 대상은 grace(10초) 를 넘긴 SUBMITTED (같은 cid, orderedAt 만 과거로)
        submittedInDb.add(new Trade(10L, cid, null, MARKET, TradeType.BUY, "market",
                BigDecimal.ZERO, new BigDecimal("248750"), null, null, null,
                TradeStatus.SUBMITTED, 50, "Auto buy signal",
                LocalDateTime.now().minusSeconds(30), null, LocalDateTime.now().minusSeconds(30), cid));
        when(privateApi.getOrderByClientOrderId(cid)).thenReturn(filledOrder("ex-99"));

        svc.reconcileSubmittedOrders(MARKET);

        // Trade DONE + 거래소 uuid + Position(SL/TP) 생성 — 무보호 창 제거
        Trade reconciled = submittedInDb.get(0);
        assertThat(reconciled.getStatus()).isEqualTo(TradeStatus.DONE);
        assertThat(reconciled.getUuid()).isEqualTo("ex-99");
        assertThat(reconciled.getExecutedVolume()).isEqualByComparingTo("248");
        assertThat(reconciled.getPositionId()).isEqualTo(77L);

        ArgumentCaptor<Position> pos = ArgumentCaptor.forClass(Position.class);
        verify(posRepo).save(pos.capture());
        assertThat(pos.getValue().getEntryPrice()).isEqualByComparingTo("1000");
        assertThat(pos.getValue().getEntryVolume()).isEqualByComparingTo("248");
        assertThat(pos.getValue().getStopLossPrice()).isEqualByComparingTo("985");
        assertThat(pos.getValue().getTakeProfitPrice()).isEqualByComparingTo("1030");
    }
}
