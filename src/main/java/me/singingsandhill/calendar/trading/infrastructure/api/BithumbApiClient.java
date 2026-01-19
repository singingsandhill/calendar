package me.singingsandhill.calendar.trading.infrastructure.api;

import me.singingsandhill.calendar.trading.infrastructure.api.dto.*;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

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
        try {
            BithumbOrderbookResponse orderbook = getOrderbook();
            if (orderbook != null && orderbook.orderbookUnits() != null && !orderbook.orderbookUnits().isEmpty()) {
                // 매수/매도 호가의 중간값
                BithumbOrderbookResponse.OrderbookUnit topUnit = orderbook.orderbookUnits().get(0);

                // Null check on individual price fields
                if (topUnit.askPrice() == null || topUnit.bidPrice() == null) {
                    log.warn("Cannot calculate current price: ask or bid price is null");
                    return null;
                }

                return (topUnit.askPrice() + topUnit.bidPrice()) / 2;
            }
            return null;
        } catch (Exception e) {
            log.error("Error calculating current price from orderbook: {}", e.getMessage());
            return null;
        }
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
     */
    public BithumbOrderResponse placeMarketBuyOrder(BigDecimal totalAmount) {
        return privateApi.placeMarketBuyOrder(tradingProperties.getBot().getMarket(), totalAmount);
    }

    /**
     * 시장가 매도
     */
    public BithumbOrderResponse placeMarketSellOrder(BigDecimal volume) {
        return privateApi.placeMarketSellOrder(tradingProperties.getBot().getMarket(), volume);
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
        try {
            List<BithumbOrderResponse> pendingOrders = getPendingOrders();
            if (pendingOrders == null || pendingOrders.isEmpty()) {
                log.debug("No pending orders to cancel");
                return;
            }

            int successCount = 0;
            int failCount = 0;

            for (BithumbOrderResponse order : pendingOrders) {
                try {
                    BithumbOrderResponse cancelResult = cancelOrder(order.uuid());
                    if (cancelResult != null) {
                        successCount++;
                        log.debug("Cancelled order: {}", order.uuid());
                    } else {
                        failCount++;
                        log.warn("Failed to cancel order: {} - API returned null", order.uuid());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("Error cancelling order {}: {}", order.uuid(), e.getMessage());
                }
            }

            log.info("Cancelled pending orders: {} succeeded, {} failed", successCount, failCount);
        } catch (Exception e) {
            log.error("Error retrieving pending orders for cancellation: {}", e.getMessage());
        }
    }
}
