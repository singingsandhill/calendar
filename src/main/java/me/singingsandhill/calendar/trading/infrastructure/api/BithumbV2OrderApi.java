package me.singingsandhill.calendar.trading.infrastructure.api;

import me.singingsandhill.calendar.trading.infrastructure.api.auth.BithumbJwtGenerator;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbV2OrderCreateResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bithumb v2 주문 생성(POST /v2/orders)·취소(DELETE /v2/order) 어댑터.
 *
 * <p>핵심 설계: v2 생성 응답에는 체결 정보(state/executed_volume/trades)가 없으므로, 생성 직후
 * {@link BithumbPrivateApi#getOrder(String)} (GET /v1/order) 재조회로 기존 {@link BithumbOrderResponse}
 * (trades 포함)를 채워 반환한다. 이로써 application 계층은 v1/v2 를 구분하지 않는다.
 *
 * <p>응답 유실(연결 종료/타임아웃)로 생성 응답을 못 받으면 재전송하지 않고 client_order_id 로
 * 접수 여부를 재조회한다(P0-2).
 *
 * <p>조회는 v1(GET /v1/order) 을 재사용하므로 인증은 {@link BithumbJwtGenerator} 를 v1 과 공유한다.
 * 근거: docs/trading-bithumb-v2-migration-plan.md
 */
@Component
public class BithumbV2OrderApi {

    private static final Logger log = LoggerFactory.getLogger(BithumbV2OrderApi.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    // 정규화 재조회(GET /v1/order) 재시도 횟수 — 접수 직후 조회 인덱싱 지연을 흡수한다.
    private static final int REQUERY_ATTEMPTS = 3;

    private final WebClient webClient;
    private final BithumbJwtGenerator jwtGenerator;
    private final BithumbPrivateApi privateApi;
    // 재시도 간 대기(밀리초, 선형 증가: base*1, base*2). 테스트에서 대기 단축용으로만 변경.
    private long requeryBackoffBaseMillis = 300;

    public BithumbV2OrderApi(TradingProperties tradingProperties,
                             WebClient.Builder webClientBuilder,
                             BithumbJwtGenerator jwtGenerator,
                             BithumbPrivateApi privateApi) {
        this.webClient = webClientBuilder
                .baseUrl(tradingProperties.getBithumb().getBaseUrl())
                .build();
        this.jwtGenerator = jwtGenerator;
        this.privateApi = privateApi;
    }

    /**
     * 시장가 매수 (총 금액 지정, order_type=price).
     */
    public BithumbOrderResponse placeMarketBuyOrder(String market, BigDecimal price, String clientOrderId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("market", market);
        params.put("side", "bid");
        params.put("order_type", "price");
        params.put("price", price.toPlainString());
        if (clientOrderId != null) {
            params.put("client_order_id", clientOrderId);
        }
        log.info("[v2] Placing market buy order for {} - amount: {}, clientOrderId: {}", market, price, clientOrderId);
        return normalize(createOrder(params), clientOrderId);
    }

    /**
     * 시장가 매도 (수량 지정, order_type=market).
     */
    public BithumbOrderResponse placeMarketSellOrder(String market, BigDecimal volume, String clientOrderId) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("market", market);
        params.put("side", "ask");
        params.put("order_type", "market");
        params.put("volume", volume.toPlainString());
        if (clientOrderId != null) {
            params.put("client_order_id", clientOrderId);
        }
        log.info("[v2] Placing market sell order for {} - volume: {}, clientOrderId: {}", market, volume, clientOrderId);
        return normalize(createOrder(params), clientOrderId);
    }

    /**
     * 주문 취소 (DELETE /v2/order, order_id 기준). v2 취소 응답도 체결/상태 정보가 부족하므로
     * GET /v1/order 재조회로 정규화한다. HTTP 에러/응답 유실 시 null — 취소는 비파괴적(멱등)이라
     * 상위가 재시도해도 안전하다. 422(주문 처리 중)는 짧은 백오프 후 1회 재시도 (계획 §5 Phase 1).
     */
    public BithumbOrderResponse cancelOrder(String orderId) {
        if (!jwtGenerator.isConfigured()) {
            log.warn("[v2] API keys not configured, skipping order cancel");
            return null;
        }
        BithumbV2OrderCreateResponse cancelled;
        try {
            cancelled = requestCancel(orderId);
        } catch (WebClientResponseException e) {
            // 422 = 주문 처리 중(체결 진행) — 짧게 기다렸다 1회만 재시도
            log.warn("[v2] cancel got 422 (order processing) — retrying once: {}", orderId);
            try {
                Thread.sleep(requeryBackoffBaseMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            }
            try {
                cancelled = requestCancel(orderId);
            } catch (WebClientResponseException e2) {
                log.error("[v2] cancel still 422 after retry — giving up: {}", orderId);
                return null;
            }
        }
        if (cancelled == null || cancelled.orderId() == null) {
            return null;
        }
        BithumbOrderResponse full = requeryWithBackoff(cancelled.orderId());
        return full != null ? full : partialResponse(cancelled);
    }

    /**
     * DELETE /v2/order 1회 전송. 422 만 예외로 던져 상위가 재시도하고, 그 외 에러는 null 로 수렴.
     */
    private BithumbV2OrderCreateResponse requestCancel(String orderId) {
        Map<String, Object> params = Map.of("order_id", orderId);
        String authToken = jwtGenerator.generateAuthorizationHeader(params);
        log.info("[v2] Cancelling order: {}", orderId);
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/order")
                        .queryParam("order_id", orderId)
                        .build())
                .header("Authorization", authToken)
                .retrieve()
                .bodyToMono(BithumbV2OrderCreateResponse.class)
                .timeout(TIMEOUT)
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException http) {
                        if (http.getStatusCode().value() == 422) {
                            return Mono.error(http); // 처리 중 — cancelOrder 가 1회 재시도
                        }
                        log.error("[v2] order cancel HTTP error: {} - {}",
                                http.getStatusCode(), http.getResponseBodyAsString());
                        return Mono.empty();
                    }
                    log.error("[v2] order cancel failed: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
    }

    private BithumbV2OrderCreateResponse createOrder(Map<String, Object> params) {
        if (!jwtGenerator.isConfigured()) {
            log.warn("[v2] API keys not configured, skipping order execution");
            return null;
        }
        String authToken = jwtGenerator.generateAuthorizationHeader(params);
        return webClient.post()
                .uri("/v2/orders")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(params)
                .retrieve()
                .bodyToMono(BithumbV2OrderCreateResponse.class)
                .timeout(TIMEOUT)
                // 타임아웃/응답유실/HTTP 에러 시 재전송하지 않는다 — 상위 normalize 가 client_order_id 로
                // 재조회한다. §8-E: 중복 client_order_id 에러도 이 경로로 흡수 — "주문 존재 가능"이므로
                // 재조회가 기존 주문을 복구한다(이중 체결 방지).
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("[v2] order create HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.empty();
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("[v2] order create failed (response may be lost): {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
    }

    /**
     * v2 생성 응답을 v1 형태({@link BithumbOrderResponse}, trades 포함)로 정규화한다.
     * 1) 응답 유실 → client_order_id 로 접수 여부 재조회 (재전송 금지).
     * 2) 정상 접수 → order_id 로 GET /v1/order 재조회해 체결 정보 확보.
     * 3) 재조회 실패 → client_order_id 재시도 → 그래도 없으면 state=UNKNOWN 부분 응답(상위 스윕이 수습).
     */
    private BithumbOrderResponse normalize(BithumbV2OrderCreateResponse created, String clientOrderId) {
        if (created == null || created.orderId() == null) {
            if (clientOrderId != null) {
                BithumbOrderResponse recovered = privateApi.getOrderByClientOrderId(clientOrderId);
                if (recovered != null) {
                    log.warn("[v2] create response lost but order found via client_order_id={}, uuid={}",
                            clientOrderId, recovered.uuid());
                    return recovered;
                }
            }
            log.warn("[v2] create response lost and not found via reconcile (clientOrderId={}) — treating as not placed",
                    clientOrderId);
            return null;
        }

        BithumbOrderResponse full = requeryWithBackoff(created.orderId());
        if (full != null) {
            return full;
        }
        if (clientOrderId != null) {
            BithumbOrderResponse byCid = privateApi.getOrderByClientOrderId(clientOrderId);
            if (byCid != null) {
                return byCid;
            }
        }
        log.warn("[v2] order accepted (order_id={}) but re-query failed — returning UNKNOWN partial response",
                created.orderId());
        return partialResponse(created);
    }

    /**
     * GET /v1/order 재조회를 최대 {@value REQUERY_ATTEMPTS}회, 선형 백오프로 시도한다.
     * 접수 직후 주문 조회가 아직 인덱싱되지 않은 짧은 지연을 흡수해 UNKNOWN 부분 응답 빈도를 줄인다.
     */
    private BithumbOrderResponse requeryWithBackoff(String orderId) {
        for (int attempt = 0; attempt < REQUERY_ATTEMPTS; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(requeryBackoffBaseMillis * attempt);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            BithumbOrderResponse full = privateApi.getOrder(orderId);
            if (full != null) {
                return full;
            }
        }
        return null;
    }

    /** 테스트 전용 — 백오프 대기 단축. */
    void setRequeryBackoffBaseMillis(long requeryBackoffBaseMillis) {
        this.requeryBackoffBaseMillis = requeryBackoffBaseMillis;
    }

    /**
     * §8-D: 접수는 확인됐으나 체결 정보 재조회에 실패한 경우의 부분 응답. state=UNKNOWN 으로 표시하고
     * 체결 내역(trades)은 비운다 — 상위(스윕/체결가 확인)가 이후 정합화한다.
     */
    private BithumbOrderResponse partialResponse(BithumbV2OrderCreateResponse created) {
        return new BithumbOrderResponse(
                created.orderId(), created.side(), created.orderType(), null, "UNKNOWN",
                created.market(), created.createdAt(), null, null, null, null, null, null, null, null, null);
    }
}
