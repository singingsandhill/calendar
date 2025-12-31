package me.singingsandhill.calendar.trading.infrastructure.api;

import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbCandleResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbOrderbookResponse;
import me.singingsandhill.calendar.trading.infrastructure.api.dto.BithumbTradeResponse;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class BithumbPublicApi {

    private static final Logger log = LoggerFactory.getLogger(BithumbPublicApi.class);

    private final WebClient webClient;

    public BithumbPublicApi(TradingProperties tradingProperties, WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl(tradingProperties.getBithumb().getBaseUrl())
                .build();
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * 분(Minute) 캔들 조회
     * @param unit 분 단위 (1, 3, 5, 10, 15, 30, 60, 240)
     * @param market 마켓 코드 (ex. KRW-ADA)
     * @param count 캔들 개수 (최대 200)
     */
    public List<BithumbCandleResponse> getMinuteCandles(int unit, String market, int count) {
        log.debug("Fetching {} minute candles for {} (count: {})", unit, market, count);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/candles/minutes/{unit}")
                        .queryParam("market", market)
                        .queryParam("count", count)
                        .build(unit))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<BithumbCandleResponse>>() {})
                .timeout(TIMEOUT)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("API error fetching candles: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error fetching candles: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();
    }

    /**
     * 분(Minute) 캔들 조회 (특정 시각까지)
     * @param unit 분 단위
     * @param market 마켓 코드
     * @param to 마지막 캔들 시각 (exclusive), ISO8061 형식
     * @param count 캔들 개수
     */
    public List<BithumbCandleResponse> getMinuteCandles(int unit, String market, String to, int count) {
        log.debug("Fetching {} minute candles for {} until {} (count: {})", unit, market, to, count);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/candles/minutes/{unit}")
                        .queryParam("market", market)
                        .queryParam("to", to)
                        .queryParam("count", count)
                        .build(unit))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<BithumbCandleResponse>>() {})
                .timeout(TIMEOUT)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("API error fetching candles (to={}): {} - {}", to, e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error fetching candles (to={}): {}", to, e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();
    }

    /**
     * 최근 체결 내역 조회
     * @param market 마켓 코드
     * @param count 체결 개수 (1~500)
     */
    public List<BithumbTradeResponse> getTrades(String market, int count) {
        log.debug("Fetching {} trades for {}", count, market);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/trades/ticks")
                        .queryParam("market", market)
                        .queryParam("count", count)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<BithumbTradeResponse>>() {})
                .timeout(TIMEOUT)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("API error fetching trades: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error fetching trades: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();
    }

    /**
     * 호가 정보 조회
     * @param markets 마켓 코드 목록 (쉼표 구분)
     */
    public List<BithumbOrderbookResponse> getOrderbook(String markets) {
        log.debug("Fetching orderbook for {}", markets);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/orderbook")
                        .queryParam("markets", markets)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<BithumbOrderbookResponse>>() {})
                .timeout(TIMEOUT)
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("API error fetching orderbook: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.just(Collections.emptyList());
                })
                .onErrorResume(Exception.class, e -> {
                    log.error("Error fetching orderbook: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                })
                .block();
    }

    /**
     * 단일 마켓 호가 정보 조회 (편의 메서드)
     */
    public BithumbOrderbookResponse getOrderbook(String market, boolean single) {
        List<BithumbOrderbookResponse> response = getOrderbook(market);
        return response != null && !response.isEmpty() ? response.get(0) : null;
    }
}
