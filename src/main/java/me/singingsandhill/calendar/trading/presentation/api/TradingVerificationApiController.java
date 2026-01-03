package me.singingsandhill.calendar.trading.presentation.api;

import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeStatus;
import me.singingsandhill.calendar.trading.domain.trade.TradeType;
import me.singingsandhill.calendar.trading.infrastructure.api.BithumbApiClient;
import me.singingsandhill.calendar.trading.infrastructure.api.auth.BithumbJwtGenerator;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderbookResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 트레이딩 API 검증용 컨트롤러
 * 가격 조회 → 주문 API → DB 저장 흐름 검증
 */
@RestController
@RequestMapping("/api/trading/verify")
public class TradingVerificationApiController {

    private static final Logger log = LoggerFactory.getLogger(TradingVerificationApiController.class);

    private final BithumbApiClient bithumbApiClient;
    private final BithumbJwtGenerator jwtGenerator;
    private final TradingProperties tradingProperties;
    private final TradeRepository tradeRepository;
    private final PositionRepository positionRepository;

    public TradingVerificationApiController(BithumbApiClient bithumbApiClient,
                                            BithumbJwtGenerator jwtGenerator,
                                            TradingProperties tradingProperties,
                                            TradeRepository tradeRepository,
                                            PositionRepository positionRepository) {
        this.bithumbApiClient = bithumbApiClient;
        this.jwtGenerator = jwtGenerator;
        this.tradingProperties = tradingProperties;
        this.tradeRepository = tradeRepository;
        this.positionRepository = positionRepository;
    }

    /**
     * Step 1: API 설정 검증
     * API 키 설정 여부 및 기본 설정 확인
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> verifyConfig() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("timestamp", LocalDateTime.now().toString());
        result.put("apiKeyConfigured", jwtGenerator.isConfigured());
        result.put("market", tradingProperties.getBot().getMarket());
        result.put("baseUrl", tradingProperties.getBithumb().getBaseUrl());
        result.put("botEnabled", tradingProperties.getBot().isEnabled());
        result.put("maxPositions", tradingProperties.getBot().getMaxPositions());
        result.put("orderRatio", tradingProperties.getBot().getOrderRatio());

        if (!jwtGenerator.isConfigured()) {
            result.put("warning", "API 키가 설정되지 않음. Private API 호출 불가.");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Step 2: 가격 조회 검증 (Public API)
     * 호가 조회 및 현재가 계산
     */
    @GetMapping("/price")
    public ResponseEntity<Map<String, Object>> verifyPrice() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("market", tradingProperties.getBot().getMarket());

        try {
            // 호가 조회
            BithumbOrderbookResponse orderbook = bithumbApiClient.getOrderbook();

            if (orderbook == null) {
                result.put("success", false);
                result.put("error", "호가 조회 실패: null 응답");
                return ResponseEntity.ok(result);
            }

            if (orderbook.orderbookUnits() == null || orderbook.orderbookUnits().isEmpty()) {
                result.put("success", false);
                result.put("error", "호가 데이터 없음");
                result.put("rawResponse", orderbook.toString());
                return ResponseEntity.ok(result);
            }

            BithumbOrderbookResponse.OrderbookUnit topUnit = orderbook.orderbookUnits().get(0);
            double askPrice = topUnit.askPrice();
            double bidPrice = topUnit.bidPrice();
            double midPrice = (askPrice + bidPrice) / 2;
            double spread = askPrice - bidPrice;
            double spreadPercent = (spread / midPrice) * 100;

            result.put("success", true);
            result.put("askPrice", askPrice);
            result.put("bidPrice", bidPrice);
            result.put("midPrice", midPrice);
            result.put("spread", spread);
            result.put("spreadPercent", String.format("%.4f%%", spreadPercent));
            result.put("orderbookDepth", orderbook.orderbookUnits().size());

            // getCurrentPrice() 메서드 직접 테스트
            Double currentPrice = bithumbApiClient.getCurrentPrice();
            result.put("getCurrentPriceResult", currentPrice);
            result.put("priceMethodMatch", currentPrice != null && Math.abs(currentPrice - midPrice) < 0.01);

        } catch (Exception e) {
            log.error("가격 조회 검증 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Step 3: 잔고 조회 검증 (Private API)
     * KRW 잔고 및 코인 잔고 확인
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> verifyBalance() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());

        if (!jwtGenerator.isConfigured()) {
            result.put("success", false);
            result.put("error", "API 키 미설정. Private API 호출 불가.");
            return ResponseEntity.ok(result);
        }

        try {
            // KRW 잔고 조회
            BithumbAccountResponse krwAccount = bithumbApiClient.getKrwBalance();
            BigDecimal krwBalance = parseBalance(krwAccount);
            result.put("krwBalance", krwBalance);
            result.put("krwBalanceSuccess", krwBalance != null);

            // 코인 잔고 조회
            String market = tradingProperties.getBot().getMarket();
            String coinCurrency = market.replace("KRW-", "");
            BithumbAccountResponse coinAccount = bithumbApiClient.getCoinBalance();
            BigDecimal coinBalance = parseBalance(coinAccount);
            result.put("coinCurrency", coinCurrency);
            result.put("coinBalance", coinBalance);
            result.put("coinBalanceSuccess", coinBalance != null);

            // 주문 가능 금액 계산
            if (krwBalance != null) {
                double orderRatio = tradingProperties.getBot().getOrderRatio();
                BigDecimal orderableAmount = krwBalance.multiply(BigDecimal.valueOf(orderRatio))
                        .setScale(0, RoundingMode.DOWN);
                result.put("orderRatio", orderRatio);
                result.put("orderableAmount", orderableAmount);
                result.put("minOrderAmount", 5000);
                result.put("canPlaceOrder", orderableAmount.compareTo(BigDecimal.valueOf(5000)) >= 0);
            }

            result.put("success", krwBalance != null || coinBalance != null);

        } catch (Exception e) {
            log.error("잔고 조회 검증 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Step 4: 테스트 주문 실행 (실제 돈 사용!)
     * 최소 금액(5,000원)으로 매수 테스트 후 DB 저장 확인
     *
     * @param amount 주문 금액 (최소 5000)
     * @param immediatelySell true이면 매수 후 즉시 매도하여 원상복구
     */
    @PostMapping("/test-order")
    public ResponseEntity<Map<String, Object>> testOrder(
            @RequestParam(defaultValue = "5500") BigDecimal amount,
            @RequestParam(defaultValue = "false") boolean immediatelySell) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());
        result.put("orderAmount", amount);
        result.put("immediatelySell", immediatelySell);

        // 최소 금액 검증
        if (amount.compareTo(BigDecimal.valueOf(5000)) < 0) {
            result.put("success", false);
            result.put("error", "최소 주문 금액은 5,000원입니다.");
            return ResponseEntity.ok(result);
        }

        // API 키 확인
        if (!jwtGenerator.isConfigured()) {
            result.put("success", false);
            result.put("error", "API 키 미설정. 주문 불가.");
            return ResponseEntity.ok(result);
        }

        String market = tradingProperties.getBot().getMarket();
        Trade buyTrade = null;
        Position position = null;

        try {
            // 1. 잔고 확인
            BithumbAccountResponse krwAccount = bithumbApiClient.getKrwBalance();
            BigDecimal krwBalance = parseBalance(krwAccount);
            result.put("krwBalanceBefore", krwBalance);

            if (krwBalance == null || krwBalance.compareTo(amount) < 0) {
                result.put("success", false);
                result.put("error", "잔고 부족. 필요: " + amount + ", 보유: " + krwBalance);
                return ResponseEntity.ok(result);
            }

            // 2. 현재가 조회 (price, stopLoss, takeProfit 계산용)
            Double currentPriceDouble = bithumbApiClient.getCurrentPrice();
            if (currentPriceDouble == null) {
                result.put("success", false);
                result.put("error", "현재가 조회 실패");
                return ResponseEntity.ok(result);
            }
            BigDecimal currentPrice = BigDecimal.valueOf(currentPriceDouble);
            BigDecimal estimatedVolume = amount.divide(currentPrice, 8, RoundingMode.DOWN);

            // 3. Trade 레코드 생성 (WAIT 상태)
            String uuid = java.util.UUID.randomUUID().toString();
            buyTrade = Trade.createBuyOrder(uuid, market, currentPrice, estimatedVolume,
                    "price", 0, "VERIFICATION_TEST");
            buyTrade = tradeRepository.save(buyTrade);
            result.put("tradeIdCreated", buyTrade.getId());
            result.put("tradeStatusAfterCreate", buyTrade.getStatus().name());

            // 3. 매수 주문 실행
            log.info("테스트 매수 주문 실행: market={}, amount={}", market, amount);
            BithumbOrderResponse orderResponse = bithumbApiClient.placeMarketBuyOrder(amount);

            if (orderResponse == null) {
                buyTrade.markFailed("주문 응답 null");
                tradeRepository.save(buyTrade);
                result.put("success", false);
                result.put("error", "주문 API 응답 null");
                result.put("tradeStatusAfterFail", buyTrade.getStatus().name());
                return ResponseEntity.ok(result);
            }

            result.put("orderUuid", orderResponse.uuid());
            result.put("orderSide", orderResponse.side());
            result.put("orderType", orderResponse.ordType());
            result.put("orderState", orderResponse.state());

            // 4. 체결 정보 추출
            BigDecimal executedPrice = extractExecutedPrice(orderResponse);
            BigDecimal executedVolume = extractExecutedVolume(orderResponse);
            BigDecimal fee = extractFee(orderResponse);

            result.put("executedPrice", executedPrice);
            result.put("executedVolume", executedVolume);
            result.put("fee", fee);

            if (executedPrice == null || executedVolume == null) {
                // 체결 정보 추출 실패 - fallback으로 현재가 사용
                Double fallbackPrice = bithumbApiClient.getCurrentPrice();
                if (fallbackPrice != null && executedPrice == null) {
                    executedPrice = BigDecimal.valueOf(fallbackPrice);
                    result.put("executedPriceFallback", true);
                }
                if (executedVolume == null && executedPrice != null) {
                    executedVolume = amount.divide(executedPrice, 8, RoundingMode.DOWN);
                    result.put("executedVolumeFallback", true);
                }
            }

            // 5. Trade 상태 업데이트
            if (executedPrice != null && executedVolume != null) {
                buyTrade.markExecuted(executedPrice, executedVolume, fee != null ? fee : BigDecimal.ZERO);
                buyTrade = tradeRepository.save(buyTrade);
                result.put("tradeStatusAfterExecute", buyTrade.getStatus().name());

                // 6. Position 생성 (stopLoss -8%, takeProfit +15%)
                BigDecimal stopLossPrice = executedPrice.multiply(BigDecimal.valueOf(0.92))
                        .setScale(0, RoundingMode.DOWN);
                BigDecimal takeProfitPrice = executedPrice.multiply(BigDecimal.valueOf(1.15))
                        .setScale(0, RoundingMode.UP);
                position = Position.open(
                        market,
                        executedPrice,
                        executedVolume,
                        stopLossPrice,
                        takeProfitPrice,
                        fee != null ? fee : BigDecimal.ZERO
                );
                position = positionRepository.save(position);
                result.put("positionIdCreated", position.getId());
                result.put("positionStatus", position.getStatus().name());
                result.put("positionEntryPrice", position.getEntryPrice());
                result.put("positionEntryVolume", position.getEntryVolume());
            } else {
                buyTrade.markFailed("체결 정보 추출 실패");
                tradeRepository.save(buyTrade);
                result.put("success", false);
                result.put("error", "체결 정보 추출 실패");
                return ResponseEntity.ok(result);
            }

            // 7. DB 저장 검증
            Trade savedTrade = tradeRepository.findById(buyTrade.getId()).orElse(null);
            Position savedPosition = position != null ?
                    positionRepository.findById(position.getId()).orElse(null) : null;

            result.put("tradeDbVerified", savedTrade != null && savedTrade.getStatus() == TradeStatus.DONE);
            result.put("positionDbVerified", savedPosition != null && savedPosition.getStatus() == PositionStatus.OPEN);

            // 8. 즉시 매도 옵션
            if (immediatelySell && executedVolume != null) {
                result.put("sellAttempt", true);

                // 매도 전 최소 금액 검증 (Bithumb 최소 주문: 5,000원)
                Double currentPriceForSell = bithumbApiClient.getCurrentPrice();
                BigDecimal sellTotalAmount = executedVolume.multiply(
                        currentPriceForSell != null ? BigDecimal.valueOf(currentPriceForSell) : executedPrice);
                result.put("sellTotalAmount", sellTotalAmount);

                if (sellTotalAmount.compareTo(BigDecimal.valueOf(5000)) < 0) {
                    result.put("sellSkipped", true);
                    result.put("sellSkipReason", "최소 주문 금액 미달 (5,000원). 현재: " + sellTotalAmount.setScale(0, RoundingMode.DOWN) + "원");
                } else {
                    BithumbOrderResponse sellResponse = bithumbApiClient.placeMarketSellOrder(executedVolume);

                    if (sellResponse != null) {
                        result.put("sellSuccess", true);
                        result.put("sellUuid", sellResponse.uuid());
                        result.put("sellState", sellResponse.state());

                        // Position 청산
                        if (savedPosition != null) {
                            BigDecimal sellPrice = extractExecutedPrice(sellResponse);
                            if (sellPrice == null) {
                                Double sellFallbackPrice = bithumbApiClient.getCurrentPrice();
                                sellPrice = sellFallbackPrice != null ? BigDecimal.valueOf(sellFallbackPrice) : executedPrice;
                            }
                            BigDecimal sellFee = extractFee(sellResponse);
                            savedPosition.close(sellPrice, executedVolume,
                                    me.singingsandhill.calendar.trading.domain.position.CloseReason.MANUAL,
                                    sellFee != null ? sellFee : BigDecimal.ZERO);
                            positionRepository.save(savedPosition);
                            result.put("positionClosed", true);
                            result.put("realizedPnl", savedPosition.getRealizedPnl());
                        }
                    } else {
                        result.put("sellSuccess", false);
                        result.put("sellError", "매도 주문 응답 null");
                    }
                }
            }

            // 9. 최종 잔고 확인
            BithumbAccountResponse krwAccountAfter = bithumbApiClient.getKrwBalance();
            BithumbAccountResponse coinAccountAfter = bithumbApiClient.getCoinBalance();
            result.put("krwBalanceAfter", parseBalance(krwAccountAfter));
            result.put("coinBalanceAfter", parseBalance(coinAccountAfter));

            result.put("success", true);

        } catch (Exception e) {
            log.error("테스트 주문 실행 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());

            // Trade 실패 처리
            if (buyTrade != null && buyTrade.getStatus() == TradeStatus.WAIT) {
                buyTrade.markFailed(e.getMessage());
                tradeRepository.save(buyTrade);
                result.put("tradeMarkedFailed", true);
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Step 5: 최근 Trade 조회
     */
    @GetMapping("/trades/recent")
    public ResponseEntity<Map<String, Object>> getRecentTrades(
            @RequestParam(defaultValue = "10") int limit) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());

        try {
            String market = tradingProperties.getBot().getMarket();
            // page=0, size=limit 형태로 호출
            List<Trade> trades = tradeRepository.findByMarketOrderByCreatedAtDesc(market, 0, limit);

            result.put("market", market);
            result.put("count", trades.size());
            result.put("trades", trades.stream().map(this::toTradeMap).toList());
            result.put("success", true);

        } catch (Exception e) {
            log.error("Trade 조회 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Step 6: 최근 Position 조회
     */
    @GetMapping("/positions/recent")
    public ResponseEntity<Map<String, Object>> getRecentPositions(
            @RequestParam(defaultValue = "10") int limit) {

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());

        try {
            String market = tradingProperties.getBot().getMarket();
            // page=0, size=limit 형태로 호출
            List<Position> positions = positionRepository.findByMarketOrderByOpenedAtDesc(market, 0, limit);

            result.put("market", market);
            result.put("count", positions.size());
            result.put("positions", positions.stream().map(this::toPositionMap).toList());
            result.put("success", true);

        } catch (Exception e) {
            log.error("Position 조회 실패", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 전체 검증 실행 (가격 + 잔고)
     * 테스트 주문은 실제 돈이 사용되므로 별도 호출 필요
     */
    @GetMapping("/full")
    public ResponseEntity<Map<String, Object>> fullVerification() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", LocalDateTime.now().toString());

        // 1. Config
        result.put("config", verifyConfig().getBody());

        // 2. Price
        result.put("price", verifyPrice().getBody());

        // 3. Balance (API 키 설정된 경우만)
        if (jwtGenerator.isConfigured()) {
            result.put("balance", verifyBalance().getBody());
        } else {
            Map<String, Object> balanceSkip = new LinkedHashMap<>();
            balanceSkip.put("skipped", true);
            balanceSkip.put("reason", "API 키 미설정");
            result.put("balance", balanceSkip);
        }

        // 4. Recent trades/positions
        result.put("recentTrades", getRecentTrades(5).getBody());
        result.put("recentPositions", getRecentPositions(5).getBody());

        return ResponseEntity.ok(result);
    }

    // === Helper Methods ===

    private BigDecimal parseBalance(BithumbAccountResponse account) {
        if (account != null && account.balance() != null && !account.balance().isEmpty()) {
            try {
                return new BigDecimal(account.balance());
            } catch (NumberFormatException e) {
                log.warn("잔고 파싱 실패: {}", account.balance());
            }
        }
        return null;
    }

    private BigDecimal extractExecutedPrice(BithumbOrderResponse response) {
        if (response.trades() != null && !response.trades().isEmpty()) {
            BithumbOrderResponse.TradeDetail firstTrade = response.trades().get(0);
            if (firstTrade.price() != null && !firstTrade.price().isEmpty()) {
                try {
                    return new BigDecimal(firstTrade.price());
                } catch (NumberFormatException e) {
                    log.warn("체결가 파싱 실패: {}", firstTrade.price());
                }
            }
        }
        return null;
    }

    private BigDecimal extractExecutedVolume(BithumbOrderResponse response) {
        if (response.trades() != null && !response.trades().isEmpty()) {
            BithumbOrderResponse.TradeDetail firstTrade = response.trades().get(0);
            if (firstTrade.volume() != null && !firstTrade.volume().isEmpty()) {
                try {
                    return new BigDecimal(firstTrade.volume());
                } catch (NumberFormatException e) {
                    log.warn("체결량 파싱 실패: {}", firstTrade.volume());
                }
            }
        }
        return null;
    }

    private BigDecimal extractFee(BithumbOrderResponse response) {
        if (response.paidFee() != null && !response.paidFee().isEmpty()) {
            try {
                return new BigDecimal(response.paidFee());
            } catch (NumberFormatException e) {
                log.warn("수수료 파싱 실패: {}", response.paidFee());
            }
        }
        return BigDecimal.ZERO;
    }

    private Map<String, Object> toTradeMap(Trade trade) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", trade.getId());
        map.put("market", trade.getMarket());
        map.put("type", trade.getTradeType().name());
        map.put("status", trade.getStatus().name());
        map.put("price", trade.getPrice());
        map.put("volume", trade.getVolume());
        map.put("executedPrice", trade.getExecutedPrice());
        map.put("executedVolume", trade.getExecutedVolume());
        map.put("fee", trade.getFee());
        map.put("createdAt", trade.getCreatedAt() != null ? trade.getCreatedAt().toString() : null);
        return map;
    }

    private Map<String, Object> toPositionMap(Position position) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", position.getId());
        map.put("market", position.getMarket());
        map.put("status", position.getStatus().name());
        map.put("entryPrice", position.getEntryPrice());
        map.put("entryVolume", position.getEntryVolume());
        map.put("entryFee", position.getEntryFee());
        map.put("exitPrice", position.getExitPrice());
        map.put("realizedPnl", position.getRealizedPnl());
        map.put("realizedPnlPct", position.getRealizedPnlPct());
        map.put("closeReason", position.getCloseReason() != null ? position.getCloseReason().name() : null);
        map.put("openedAt", position.getOpenedAt() != null ? position.getOpenedAt().toString() : null);
        map.put("closedAt", position.getClosedAt() != null ? position.getClosedAt().toString() : null);
        return map;
    }
}
