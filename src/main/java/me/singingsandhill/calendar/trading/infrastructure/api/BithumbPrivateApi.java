package me.singingsandhill.calendar.trading.infrastructure.api;

import me.singingsandhill.calendar.trading.infrastructure.api.auth.BithumbJwtGenerator;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbAccountResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderChanceResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class BithumbPrivateApi {

    private static final Logger log = LoggerFactory.getLogger(BithumbPrivateApi.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    // Issue #7: Rate Limit 재시도 설정
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final double RETRY_BACKOFF_MULTIPLIER = 2.0;

    private final WebClient webClient;
    private final BithumbJwtGenerator jwtGenerator;

    public BithumbPrivateApi(TradingProperties tradingProperties,
                              WebClient.Builder webClientBuilder,
                              BithumbJwtGenerator jwtGenerator) {
        this.webClient = webClientBuilder
                .baseUrl(tradingProperties.getBithumb().getBaseUrl())
                .build();
        this.jwtGenerator = jwtGenerator;
    }

    /**
     * 전체 계좌 조회
     */
    public List<BithumbAccountResponse> getAccounts() {
        log.debug("Fetching accounts");

        if (!jwtGenerator.isConfigured()) {
            log.warn("API keys not configured, skipping account fetch");
            return Collections.emptyList();
        }

        String authToken = jwtGenerator.generateAuthorizationHeader();

        return webClient.get()
                .uri("/v1/accounts")
                .header("Authorization", authToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<BithumbAccountResponse>>() {})
                .timeout(TIMEOUT)
                .onErrorResume(WebClientResponseException.class, e -> {
                    logApiError("fetching accounts", e);
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error fetching accounts: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();
    }

    private void logApiError(String operation, WebClientResponseException e) {
        if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.error("Authentication failed while {}: {} - Check API keys", operation, e.getResponseBodyAsString());
        } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            log.warn("Rate limited while {}: {}", operation, e.getResponseBodyAsString());
        } else {
            log.error("API error while {}: {} - {}", operation, e.getStatusCode(), e.getResponseBodyAsString());
        }
    }

    /**
     * 특정 화폐 잔고 조회
     */
    public BithumbAccountResponse getAccount(String currency) {
        List<BithumbAccountResponse> accounts = getAccounts();
        return accounts.stream()
                .filter(account -> currency.equals(account.currency()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 주문 가능 정보 조회
     */
    public BithumbOrderChanceResponse getOrderChance(String market) {
        log.debug("Fetching order chance for {}", market);

        if (!jwtGenerator.isConfigured()) {
            log.warn("API keys not configured, skipping order chance fetch");
            return null;
        }

        Map<String, Object> params = Map.of("market", market);
        String authToken = jwtGenerator.generateAuthorizationHeader(params);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/orders/chance")
                        .queryParam("market", market)
                        .build())
                .header("Authorization", authToken)
                .retrieve()
                .bodyToMono(BithumbOrderChanceResponse.class)
                .timeout(TIMEOUT)
                .onErrorResume(WebClientResponseException.class, e -> {
                    logApiError("fetching order chance", e);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error fetching order chance: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
    }

    /**
     * 지정가 주문
     * @param market 마켓 코드 (ex. KRW-ADA)
     * @param side 주문 종류 (bid: 매수, ask: 매도)
     * @param volume 주문 수량
     * @param price 주문 가격
     */
    public BithumbOrderResponse placeLimitOrder(String market, String side, BigDecimal volume, BigDecimal price) {
        log.info("Placing limit {} order for {} - volume: {}, price: {}", side, market, volume, price);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("market", market);
        params.put("side", side);
        params.put("volume", volume.toPlainString());
        params.put("price", price.toPlainString());
        params.put("ord_type", "limit");

        return executeOrder(params);
    }

    /**
     * 시장가 매수 (총 금액 지정)
     * @param market 마켓 코드
     * @param price 총 매수 금액
     */
    public BithumbOrderResponse placeMarketBuyOrder(String market, BigDecimal price) {
        log.info("Placing market buy order for {} - total amount: {}", market, price);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("market", market);
        params.put("side", "bid");
        params.put("price", price.toPlainString());
        params.put("ord_type", "price");

        return executeOrder(params);
    }

    /**
     * 시장가 매도 (수량 지정)
     * @param market 마켓 코드
     * @param volume 매도 수량
     */
    public BithumbOrderResponse placeMarketSellOrder(String market, BigDecimal volume) {
        log.info("Placing market sell order for {} - volume: {}", market, volume);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("market", market);
        params.put("side", "ask");
        params.put("volume", volume.toPlainString());
        params.put("ord_type", "market");

        return executeOrder(params);
    }

    private BithumbOrderResponse executeOrder(Map<String, Object> params) {
        if (!jwtGenerator.isConfigured()) {
            log.warn("API keys not configured, skipping order execution");
            return null;
        }

        // Issue #7: Rate Limit 재시도 로직
        return executeWithRetry(() -> {
            String authToken = jwtGenerator.generateAuthorizationHeader(params);

            return webClient.post()
                    .uri("/v1/orders")
                    .header("Authorization", authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(params)
                    .retrieve()
                    .bodyToMono(BithumbOrderResponse.class)
                    .timeout(TIMEOUT);
        }, "executing order");
    }

    /**
     * Issue #7: 지수 백오프 재시도 로직
     * Rate Limit (429) 발생 시 최대 3회까지 재시도
     */
    private <T> T executeWithRetry(java.util.function.Supplier<Mono<T>> operation, String operationName) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                T result = operation.get().block();
                if (attempt > 0) {
                    log.info("{} succeeded on retry attempt {}", operationName, attempt);
                }
                return result;
            } catch (WebClientResponseException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS && attempt < MAX_RETRIES) {
                    long waitTime = (long) (RETRY_DELAY_MS * Math.pow(RETRY_BACKOFF_MULTIPLIER, attempt));
                    log.warn("Rate limited while {}, retrying in {}ms (attempt {}/{})",
                            operationName, waitTime, attempt + 1, MAX_RETRIES);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted while {}", operationName);
                        return null;
                    }
                } else {
                    logApiError(operationName, e);
                    return null;
                }
            } catch (Exception e) {
                log.error("Error {}: {}", operationName, e.getMessage());
                return null;
            }
        }
        log.error("Max retries ({}) exceeded for {}", MAX_RETRIES, operationName);
        return null;
    }

    /**
     * 주문 리스트 조회
     * @param market 마켓 코드 (선택)
     * @param state 주문 상태 (wait, done, cancel)
     * @param page 페이지 수
     * @param limit 개수 제한 (최대 100)
     */
    public List<BithumbOrderResponse> getOrders(String market, String state, int page, int limit) {
        log.debug("Fetching orders - market: {}, state: {}, page: {}, limit: {}", market, state, page, limit);

        if (!jwtGenerator.isConfigured()) {
            log.warn("API keys not configured, skipping orders fetch");
            return Collections.emptyList();
        }

        Map<String, Object> params = new LinkedHashMap<>();
        if (market != null) params.put("market", market);
        if (state != null) params.put("state", state);
        params.put("page", page);
        params.put("limit", limit);
        params.put("order_by", "desc");

        String authToken = jwtGenerator.generateAuthorizationHeader(params);

        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/v1/orders");
                    if (market != null) builder.queryParam("market", market);
                    if (state != null) builder.queryParam("state", state);
                    builder.queryParam("page", page);
                    builder.queryParam("limit", limit);
                    builder.queryParam("order_by", "desc");
                    return builder.build();
                })
                .header("Authorization", authToken)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<BithumbOrderResponse>>() {})
                .timeout(TIMEOUT)
                .onErrorResume(WebClientResponseException.class, e -> {
                    logApiError("fetching orders", e);
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error fetching orders: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();
    }

    /**
     * 개별 주문 조회
     */
    public BithumbOrderResponse getOrder(String uuid) {
        log.debug("Fetching order: {}", uuid);

        if (!jwtGenerator.isConfigured()) {
            log.warn("API keys not configured, skipping order fetch");
            return null;
        }

        Map<String, Object> params = Map.of("uuid", uuid);
        String authToken = jwtGenerator.generateAuthorizationHeader(params);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/order")
                        .queryParam("uuid", uuid)
                        .build())
                .header("Authorization", authToken)
                .retrieve()
                .bodyToMono(BithumbOrderResponse.class)
                .timeout(TIMEOUT)
                .onErrorResume(WebClientResponseException.class, e -> {
                    logApiError("fetching order " + uuid, e);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error fetching order {}: {}", uuid, e.getMessage());
                    return Mono.empty();
                })
                .block();
    }

    /**
     * 주문 취소
     */
    public BithumbOrderResponse cancelOrder(String uuid) {
        log.info("Cancelling order: {}", uuid);

        if (!jwtGenerator.isConfigured()) {
            log.warn("API keys not configured, skipping order cancellation");
            return null;
        }

        Map<String, Object> params = Map.of("uuid", uuid);
        String authToken = jwtGenerator.generateAuthorizationHeader(params);

        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/order")
                        .queryParam("uuid", uuid)
                        .build())
                .header("Authorization", authToken)
                .retrieve()
                .bodyToMono(BithumbOrderResponse.class)
                .timeout(TIMEOUT)
                .onErrorResume(WebClientResponseException.class, e -> {
                    logApiError("cancelling order " + uuid, e);
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error cancelling order {}: {}", uuid, e.getMessage());
                    return Mono.empty();
                })
                .block();
    }
}
