package me.singingsandhill.calendar.stock.infrastructure.api;

import me.singingsandhill.calendar.stock.infrastructure.api.dto.*;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 한국투자증권 API 통합 클라이언트
 * - 인증, 시세조회, 계좌조회, 주문 기능을 통합 제공
 */
@Component
public class KoreaInvestmentApiClient {

    private static final Logger log = LoggerFactory.getLogger(KoreaInvestmentApiClient.class);

    private final KisAuthService authService;
    private final KisRestClient restClient;
    private final StockProperties stockProperties;

    public KoreaInvestmentApiClient(KisAuthService authService,
                                     KisRestClient restClient,
                                     StockProperties stockProperties) {
        this.authService = authService;
        this.restClient = restClient;
        this.stockProperties = stockProperties;
    }

    // ========== Authentication ==========

    /**
     * API 설정 여부 확인
     */
    public boolean isConfigured() {
        return authService.isConfigured();
    }

    /**
     * 인증 여부 확인
     */
    public boolean isAuthenticated() {
        return authService.getAccessToken() != null;
    }

    /**
     * 토큰 폐기 (종료 시 호출)
     */
    public void revokeToken() {
        authService.revokeToken();
    }

    // ========== Market Data ==========

    /**
     * 주식현재가 시세 조회
     */
    public KisQuoteResponse getQuote(String stockCode) {
        return restClient.getQuote(stockCode);
    }

    /**
     * 호가 조회
     */
    public KisOrderbookResponse getOrderbook(String stockCode) {
        return restClient.getOrderbook(stockCode);
    }

    /**
     * 일자별 시세 조회
     */
    public List<KisDailyPriceResponse> getDailyPrices(String stockCode, int days) {
        return restClient.getDailyPrices(stockCode, days);
    }

    /**
     * 전일 종가 조회
     */
    public BigDecimal getPreviousClose(String stockCode) {
        try {
            List<KisDailyPriceResponse> prices = getDailyPrices(stockCode, 1);
            if (prices == null || prices.isEmpty()) {
                log.warn("No daily price data available for stock: {}", stockCode);
                return null;
            }

            KisDailyPriceResponse firstPrice = prices.get(0);
            if (firstPrice == null || firstPrice.closePrice() == null) {
                log.warn("Invalid price data for stock: {}", stockCode);
                return null;
            }

            return firstPrice.closePrice();
        } catch (Exception e) {
            log.error("Error getting previous close for {}: {}", stockCode, e.getMessage());
            return null;
        }
    }

    /**
     * 갭 비율 계산
     */
    public BigDecimal calculateGapPercent(String stockCode) {
        KisQuoteResponse quote = getQuote(stockCode);
        if (quote != null) {
            return quote.calculateGapPercent();
        }
        return null;
    }

    /**
     * 체결강도 조회
     */
    public BigDecimal getTradeStrength(String stockCode) {
        KisQuoteResponse quote = getQuote(stockCode);
        if (quote != null) {
            return quote.calculateTradeStrength();
        }
        return null;
    }

    /**
     * 스프레드 비율 조회
     */
    public BigDecimal getSpreadPercent(String stockCode) {
        KisOrderbookResponse orderbook = getOrderbook(stockCode);
        if (orderbook != null) {
            return orderbook.calculateSpreadPercent();
        }
        return null;
    }

    // ========== Account ==========

    /**
     * 계좌 잔고 조회
     */
    public KisBalanceResponse getBalance() {
        return restClient.getBalance();
    }

    /**
     * 예수금 조회
     */
    public BigDecimal getAvailableCash() {
        try {
            KisBalanceResponse balance = getBalance();
            if (balance == null || balance.summary() == null || balance.summary().isEmpty()) {
                log.warn("No balance data available");
                return BigDecimal.ZERO;
            }

            KisBalanceResponse.AccountSummary summary = balance.summary().get(0);
            if (summary == null || summary.availableDeposit() == null) {
                log.warn("Invalid balance summary data");
                return BigDecimal.ZERO;
            }

            return summary.availableDeposit();
        } catch (Exception e) {
            log.error("Error getting available cash: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * 매수 가능 수량 조회
     */
    public int getBuyableQuantity(String stockCode, BigDecimal price) {
        KisBuyingPowerResponse response = restClient.getBuyingPower(stockCode, price);
        if (response != null) {
            return response.getMaxBuyQuantity();
        }
        return 0;
    }

    /**
     * 보유 종목 조회
     */
    public List<KisBalanceResponse.HoldingStock> getHoldings() {
        KisBalanceResponse balance = getBalance();
        if (balance != null && balance.holdings() != null) {
            return balance.holdings();
        }
        return List.of();
    }

    /**
     * 특정 종목 보유 수량 조회
     */
    public int getHoldingQuantity(String stockCode) {
        return getHoldings().stream()
            .filter(h -> stockCode.equals(h.stockCode()))
            .map(KisBalanceResponse.HoldingStock::quantity)
            .findFirst()
            .orElse(0);
    }

    // ========== Orders ==========

    /**
     * 시장가 매수
     */
    public KisOrderResponse buyMarket(String stockCode, int quantity) {
        log.info("Executing market buy: {} x {}", stockCode, quantity);
        return restClient.placeBuyOrder(stockCode, quantity, BigDecimal.ZERO, true);
    }

    /**
     * 지정가 매수
     */
    public KisOrderResponse buyLimit(String stockCode, int quantity, BigDecimal price) {
        log.info("Executing limit buy: {} x {} @ {}", stockCode, quantity, price);
        return restClient.placeBuyOrder(stockCode, quantity, price, false);
    }

    /**
     * 시장가 매도
     */
    public KisOrderResponse sellMarket(String stockCode, int quantity) {
        log.info("Executing market sell: {} x {}", stockCode, quantity);
        return restClient.placeSellOrder(stockCode, quantity, BigDecimal.ZERO, true);
    }

    /**
     * 지정가 매도
     */
    public KisOrderResponse sellLimit(String stockCode, int quantity, BigDecimal price) {
        log.info("Executing limit sell: {} x {} @ {}", stockCode, quantity, price);
        return restClient.placeSellOrder(stockCode, quantity, price, false);
    }

    /**
     * 주문 내역 조회
     */
    public KisOrderDetailResponse getOrderHistory(LocalDate date) {
        return restClient.getOrderHistory(date);
    }

    /**
     * 당일 주문 내역 조회
     */
    public KisOrderDetailResponse getTodayOrders() {
        return getOrderHistory(LocalDate.now());
    }

    // ========== Utility ==========

    /**
     * API 설정 정보 요약
     */
    public String getConfigSummary() {
        boolean configured = isConfigured();
        boolean authenticated = isAuthenticated();
        String mode = stockProperties.getKis().isProduction() ? "실전" : "모의";

        return String.format("KIS API [%s] - Configured: %s, Authenticated: %s",
            mode, configured, authenticated);
    }
}
