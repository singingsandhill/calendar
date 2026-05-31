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
    private final TradingProperties tradingProperties;

    public BithumbApiClient(BithumbPublicApi publicApi,
                            BithumbPrivateApi privateApi,
                            TradingProperties tradingProperties) {
        this.publicApi = publicApi;
        this.privateApi = privateApi;
        this.tradingProperties = tradingProperties;
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
     */
    public BithumbOrderResponse placeLimitBuyOrder(BigDecimal volume, BigDecimal price) {
        return privateApi.placeLimitOrder(tradingProperties.getBot().getMarket(), "bid", volume, price);
    }

    /**
     * 지정가 매도
     */
    public BithumbOrderResponse placeLimitSellOrder(BigDecimal volume, BigDecimal price) {
        return privateApi.placeLimitOrder(tradingProperties.getBot().getMarket(), "ask", volume, price);
    }

    /**
     * 시장가 매수
     * P0-1: LIVE 가 아니면 실주문 대신 현재가 기반 인메모리 체결을 반환한다.
     */
    public BithumbOrderResponse placeMarketBuyOrder(BigDecimal totalAmount) {
        String market = tradingProperties.getBot().getMarket();
        if (!isLive()) {
            return simulateBuy(market, totalAmount);
        }
        return privateApi.placeMarketBuyOrder(market, totalAmount);
    }

    /**
     * 시장가 매도
     * P0-1: LIVE 가 아니면 실주문 대신 현재가 기반 인메모리 체결을 반환한다.
     */
    public BithumbOrderResponse placeMarketSellOrder(BigDecimal volume) {
        String market = tradingProperties.getBot().getMarket();
        if (!isLive()) {
            return simulateSell(market, volume);
        }
        return privateApi.placeMarketSellOrder(market, volume);
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
     */
    public List<BithumbOrderResponse> getPendingOrders() {
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
     */
    public BithumbOrderResponse cancelOrder(String uuid) {
        return privateApi.cancelOrder(uuid);
    }

    /**
     * 모든 대기 중인 주문 취소
     */
    public void cancelAllPendingOrders() {
        List<BithumbOrderResponse> pendingOrders = getPendingOrders();
        if (pendingOrders != null) {
            for (BithumbOrderResponse order : pendingOrders) {
                cancelOrder(order.uuid());
            }
        }
    }
}
