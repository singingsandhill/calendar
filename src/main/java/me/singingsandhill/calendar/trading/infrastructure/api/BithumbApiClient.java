package me.singingsandhill.calendar.trading.infrastructure.api;

import me.singingsandhill.calendar.trading.infrastructure.api.dto.*;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 빗썸 API 통합 클라이언트
 * Public API와 Private API를 하나로 통합하여 제공
 */
@Component
public class BithumbApiClient {

    private static final Logger log = LoggerFactory.getLogger(BithumbApiClient.class);

    private final BithumbPublicApi publicApi;
    private final BithumbPrivateApi privateApi;
    private final BithumbV2OrderApi v2OrderApi;
    private final TradingProperties tradingProperties;

    public BithumbApiClient(BithumbPublicApi publicApi,
                            BithumbPrivateApi privateApi,
                            BithumbV2OrderApi v2OrderApi,
                            TradingProperties tradingProperties) {
        this.publicApi = publicApi;
        this.privateApi = privateApi;
        this.v2OrderApi = v2OrderApi;
        this.tradingProperties = tradingProperties;
    }

    /** Phase 1: 주문 생성/취소를 v2 API 로 라우팅할지 여부. */
    private boolean isV2() {
        return tradingProperties.getBithumb().getOrderApiVersion() == TradingProperties.Bithumb.OrderApiVersion.V2;
    }

    // ==================== Public API ====================

    /**
     * 1분 캔들 조회 (기본 마켓 사용)
     */
    public List<BithumbCandleResponse> getMinuteCandles(int count) {
        return publicApi.getMinuteCandles(1, tradingProperties.getBot().getMarket(), count);
    }

    /**
     * 1분 캔들 조회 (마켓 지정)
     */
    public List<BithumbCandleResponse> getMinuteCandles(String market, int count) {
        return publicApi.getMinuteCandles(1, market, count);
    }

    /**
     * N분 캔들 조회
     */
    public List<BithumbCandleResponse> getMinuteCandles(int unit, String market, int count) {
        return publicApi.getMinuteCandles(unit, market, count);
    }

    /**
     * 최근 체결 내역 조회
     */
    public List<BithumbTradeResponse> getTrades(int count) {
        return publicApi.getTrades(tradingProperties.getBot().getMarket(), count);
    }

    /**
     * 호가 정보 조회
     */
    public BithumbOrderbookResponse getOrderbook() {
        return publicApi.getOrderbook(tradingProperties.getBot().getMarket(), true);
    }

    /**
     * 현재가 조회 (호가 기반)
     */
    public Double getCurrentPrice() {
        BithumbOrderbookResponse orderbook = getOrderbook();
        if (orderbook != null && orderbook.orderbookUnits() != null && !orderbook.orderbookUnits().isEmpty()) {
            // 매수/매도 호가의 중간값
            BithumbOrderbookResponse.OrderbookUnit topUnit = orderbook.orderbookUnits().get(0);
            return (topUnit.askPrice() + topUnit.bidPrice()) / 2;
        }
        return null;
    }

    // ==================== Private API ====================

    /**
     * 전체 계좌 조회
     */
    public List<BithumbAccountResponse> getAccounts() {
        return privateApi.getAccounts();
    }

    /**
     * KRW 잔고 조회
     */
    public BithumbAccountResponse getKrwBalance() {
        return privateApi.getAccount("KRW");
    }

    /**
     * ADA 잔고 조회 (기본 마켓 기준)
     */
    public BithumbAccountResponse getCoinBalance() {
        String market = tradingProperties.getBot().getMarket();
        String currency = market.replace("KRW-", "");
        return privateApi.getAccount(currency);
    }

    /**
     * 주문 가능 정보 조회
     */
    public BithumbOrderChanceResponse getOrderChance() {
        return privateApi.getOrderChance(tradingProperties.getBot().getMarket());
    }

    /**
     * 지정가 매수
     * §8-A: LIVE 가 아니면 실주문을 전송하지 않는다(현재 트레이딩 플로우는 시장가만 사용).
     * Phase 0b: 플래그 ON 이면 cid 부착 + null 응답 시 재조회 (시장가와 동일 P0-2 의미론).
     */
    public BithumbOrderResponse placeLimitBuyOrder(BigDecimal volume, BigDecimal price) {
        if (!isLive()) {
            log.info("[{}] skipping limit BUY order (mode guard)", tradingProperties.getBot().getMode());
            return null;
        }
        return placeLimitOrderWithIdempotency("bid", volume, price);
    }

    /**
     * 지정가 매도
     * §8-A: LIVE 가 아니면 실주문을 전송하지 않는다.
     */
    public BithumbOrderResponse placeLimitSellOrder(BigDecimal volume, BigDecimal price) {
        if (!isLive()) {
            log.info("[{}] skipping limit SELL order (mode guard)", tradingProperties.getBot().getMode());
            return null;
        }
        return placeLimitOrderWithIdempotency("ask", volume, price);
    }

    /**
     * 지정가 주문은 미사용 경로라 v2 라우팅 없이 v1 유지 — cid 부착·재조회(P0-2)만 시장가와 동일하게.
     */
    private BithumbOrderResponse placeLimitOrderWithIdempotency(String side, BigDecimal volume, BigDecimal price) {
        String market = tradingProperties.getBot().getMarket();
        if (!tradingProperties.getBithumb().isClientOrderIdEnabled()) {
            return privateApi.placeLimitOrder(market, side, volume, price);
        }
        String cid = newClientOrderId();
        BithumbOrderResponse response = privateApi.placeLimitOrder(market, side, volume, price, cid);
        return response != null ? response : reconcileByClientOrderId(cid, "LIMIT-" + side.toUpperCase());
    }

    /**
     * 시장가 매수
     * P0-1: LIVE 가 아니면 실주문 대신 현재가 기반 인메모리 체결을 반환한다.
     */
    public BithumbOrderResponse placeMarketBuyOrder(BigDecimal totalAmount) {
        return placeMarketBuyOrder(totalAmount, newClientOrderId());
    }

    /**
     * 시장가 매수 — 호출자(서비스)가 부여한 client_order_id 사용.
     * §8-B: 선영속화(Trade SUBMITTED + cid 저장)가 주문 전송보다 먼저 일어나야 하므로 cid 소유권은
     * 서비스에 있다. v1 + clientOrderIdEnabled=false 면 cid 를 거래소로 보내지 않는다(지원 미검증).
     */
    public BithumbOrderResponse placeMarketBuyOrder(BigDecimal totalAmount, String clientOrderId) {
        String market = tradingProperties.getBot().getMarket();
        if (!isLive()) {
            return simulateBuy(market, totalAmount);
        }
        if (isV2()) {
            // v2 는 client_order_id 를 공식 지원 — 항상 부착. 정규화·재조회는 v2 어댑터가 내부 처리.
            return v2OrderApi.placeMarketBuyOrder(market, totalAmount, clientOrderId);
        }
        if (!tradingProperties.getBithumb().isClientOrderIdEnabled()) {
            return privateApi.placeMarketBuyOrder(market, totalAmount);
        }
        // P0-2(v1): 멱등키 부착 + 응답 null(타임아웃 등) 시 재전송이 아니라 재조회로 접수 여부 확인
        BithumbOrderResponse response = privateApi.placeMarketBuyOrder(market, totalAmount, clientOrderId);
        return response != null ? response : reconcileByClientOrderId(clientOrderId, "BUY");
    }

    /**
     * 시장가 매도
     * P0-1: LIVE 가 아니면 실주문 대신 현재가 기반 인메모리 체결을 반환한다.
     */
    public BithumbOrderResponse placeMarketSellOrder(BigDecimal volume) {
        return placeMarketSellOrder(volume, newClientOrderId());
    }

    /**
     * 시장가 매도 — 호출자(서비스)가 부여한 client_order_id 사용 (§8-B 매도 선영속화용, 매수와 동일).
     */
    public BithumbOrderResponse placeMarketSellOrder(BigDecimal volume, String clientOrderId) {
        String market = tradingProperties.getBot().getMarket();
        if (!isLive()) {
            return simulateSell(market, volume);
        }
        if (isV2()) {
            return v2OrderApi.placeMarketSellOrder(market, volume, clientOrderId);
        }
        if (!tradingProperties.getBithumb().isClientOrderIdEnabled()) {
            return privateApi.placeMarketSellOrder(market, volume);
        }
        // P0-2(v1): 멱등키 부착 + 응답 null 시 재조회로 접수 여부 확인 (재전송 금지)
        BithumbOrderResponse response = privateApi.placeMarketSellOrder(market, volume, clientOrderId);
        return response != null ? response : reconcileByClientOrderId(clientOrderId, "SELL");
    }

    /**
     * P0-2: 클라이언트 주문 식별자 생성. Bithumb 제약(1~36자, [A-Za-z0-9-_]) 을 만족.
     * 버전 프리픽스("t1-"/"t2-") + UUID(하이픈 제거, 32자) = 35자 — 어떤 API 버전으로 생성된
     * 주문인지 cid 만으로 추적(§6 호환성). §8-B: 서비스가 선영속화용 cid 를 만들 때도 사용.
     */
    public String newClientOrderId() {
        return (isV2() ? "t2-" : "t1-") + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * §8-B: client_order_id 가 실제로 주문에 부착되는 구성인지 여부 — 선영속화+스윕 게이트.
     * v2 는 공식 지원이라 항상 부착, v1 은 지원 미검증이라 clientOrderIdEnabled 일 때만.
     */
    public boolean supportsClientOrderId() {
        return isV2() || tradingProperties.getBithumb().isClientOrderIdEnabled();
    }

    /**
     * §8-B: client_order_id 로 개별 주문 조회 (틱 스윕용, v1/v2 공통 GET /v1/order).
     * §8-A: LIVE 가 아니면 실계정을 조회하지 않는다.
     */
    public BithumbOrderResponse getOrderByClientOrderId(String clientOrderId) {
        if (!isLive()) {
            return null;
        }
        return privateApi.getOrderByClientOrderId(clientOrderId);
    }

    /**
     * P0-2: 주문 응답이 null(응답 유실/타임아웃) 일 때, client_order_id 로 실제 접수 여부를 재조회한다.
     * 발견되면 그 주문을 반환(복구 — 재주문하지 않음), 없으면 null(진짜 실패 — 무작정 재전송하지 않음).
     */
    private BithumbOrderResponse reconcileByClientOrderId(String cid, String side) {
        BithumbOrderResponse recovered = privateApi.getOrderByClientOrderId(cid);
        if (recovered != null) {
            log.warn("[client_order_id] {} order response was null but order found via reconcile: cid={}, uuid={}",
                    side, cid, recovered.uuid());
            return recovered;
        }
        log.warn("[client_order_id] {} order response null and not found via reconcile — treating as not placed: cid={}",
                side, cid);
        return null;
    }

    // ==================== Mode Gate / Paper Simulation (P0-1) ====================

    /**
     * LIVE 모드 여부. PAPER/BACKTEST 는 실주문을 전송하지 않는다.
     */
    public boolean isLive() {
        return tradingProperties.getBot().getMode() == TradingProperties.Bot.Mode.LIVE;
    }

    /**
     * 시장가 매수 시뮬레이션 — 현재가 + 슬리피지로 보수적 체결가를 산정한다.
     */
    private BithumbOrderResponse simulateBuy(String market, BigDecimal totalAmount) {
        Double currentPrice = getCurrentPrice();
        if (currentPrice == null || totalAmount == null) {
            log.warn("[{}] Cannot simulate buy - no current price/amount", tradingProperties.getBot().getMode());
            return null;
        }
        BigDecimal slip = BigDecimal.valueOf(tradingProperties.getRisk().getSlippageBuffer());
        BigDecimal feeRate = BigDecimal.valueOf(tradingProperties.getRisk().getTakerFeeRate());
        BigDecimal fillPrice = BigDecimal.valueOf(currentPrice).multiply(BigDecimal.ONE.add(slip));
        BigDecimal volume = totalAmount.divide(fillPrice, 8, RoundingMode.DOWN);
        BigDecimal fee = totalAmount.multiply(feeRate);
        return simulatedResponse(market, "bid", "price", fillPrice, volume, totalAmount, fee);
    }

    /**
     * 시장가 매도 시뮬레이션 — 현재가 − 슬리피지로 보수적 체결가를 산정한다.
     */
    private BithumbOrderResponse simulateSell(String market, BigDecimal volume) {
        Double currentPrice = getCurrentPrice();
        if (currentPrice == null || volume == null) {
            log.warn("[{}] Cannot simulate sell - no current price/volume", tradingProperties.getBot().getMode());
            return null;
        }
        BigDecimal slip = BigDecimal.valueOf(tradingProperties.getRisk().getSlippageBuffer());
        BigDecimal feeRate = BigDecimal.valueOf(tradingProperties.getRisk().getTakerFeeRate());
        BigDecimal fillPrice = BigDecimal.valueOf(currentPrice).multiply(BigDecimal.ONE.subtract(slip));
        BigDecimal funds = fillPrice.multiply(volume);
        BigDecimal fee = funds.multiply(feeRate);
        return simulatedResponse(market, "ask", "market", fillPrice, volume, funds, fee);
    }

    private BithumbOrderResponse simulatedResponse(String market, String side, String ordType,
                                                   BigDecimal fillPrice, BigDecimal volume,
                                                   BigDecimal funds, BigDecimal fee) {
        String uuid = "PAPER-" + UUID.randomUUID();
        String now = LocalDateTime.now().toString();
        BithumbOrderResponse.TradeDetail trade = new BithumbOrderResponse.TradeDetail(
                market, uuid, fillPrice.toPlainString(), volume.toPlainString(),
                funds.toPlainString(), side, now);
        log.info("[{}] simulated {} order: market={}, fill={}, volume={}, fee={}",
                tradingProperties.getBot().getMode(), side, market, fillPrice, volume, fee);
        return new BithumbOrderResponse(
                uuid, side, ordType, fillPrice.toPlainString(), "done", market, now,
                volume.toPlainString(), "0", "0", "0", fee.toPlainString(), "0",
                volume.toPlainString(), 1, List.of(trade));
    }

    /**
     * 대기 중인 주문 목록 조회
     * §8-A: LIVE 가 아니면 실계정을 조회하지 않는다(PAPER/BACKTEST 는 미결 주문이 없음).
     */
    public List<BithumbOrderResponse> getPendingOrders() {
        if (!isLive()) {
            return List.of();
        }
        return privateApi.getOrders(tradingProperties.getBot().getMarket(), "wait", 1, 100);
    }

    /**
     * 완료된 주문 목록 조회
     */
    public List<BithumbOrderResponse> getCompletedOrders(int page, int limit) {
        return privateApi.getOrders(tradingProperties.getBot().getMarket(), "done", page, limit);
    }

    /**
     * 개별 주문 조회
     */
    public BithumbOrderResponse getOrder(String uuid) {
        return privateApi.getOrder(uuid);
    }

    /**
     * 주문 취소
     * §8-A: LIVE 가 아니면 실계정 취소를 전송하지 않는다.
     * Phase 1: v2 구성이면 DELETE /v2/order 로 라우팅 (v1 은 기존 DELETE /v1/order).
     */
    public BithumbOrderResponse cancelOrder(String uuid) {
        if (!isLive()) {
            log.info("[{}] skipping order cancel {} (mode guard)", tradingProperties.getBot().getMode(), uuid);
            return null;
        }
        if (isV2()) {
            return v2OrderApi.cancelOrder(uuid);
        }
        return privateApi.cancelOrder(uuid);
    }

    /**
     * 모든 대기 중인 주문 취소
     * §8-A: LIVE 가 아니면 no-op (getPendingOrders/cancelOrder 도 게이트되지만 명시적으로 단락).
     */
    public void cancelAllPendingOrders() {
        if (!isLive()) {
            log.info("[{}] skipping cancelAllPendingOrders (mode guard)", tradingProperties.getBot().getMode());
            return;
        }
        List<BithumbOrderResponse> pendingOrders = getPendingOrders();
        if (pendingOrders != null) {
            for (BithumbOrderResponse order : pendingOrders) {
                cancelOrder(order.uuid());
            }
        }
    }
}
