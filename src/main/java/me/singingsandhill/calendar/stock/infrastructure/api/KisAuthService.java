package me.singingsandhill.calendar.stock.infrastructure.api;

import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisHashkeyResponse;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisTokenResponse;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 한국투자증권 API 인증 서비스
 * - 접근토큰 발급/갱신/폐기
 * - Hashkey 생성 (POST 요청용)
 */
@Component
public class KisAuthService {

    private static final Logger log = LoggerFactory.getLogger(KisAuthService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int TOKEN_REFRESH_BUFFER_MINUTES = 30;

    // 재시도 설정
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    private final WebClient webClient;
    private final StockProperties stockProperties;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String accessToken;
    private volatile LocalDateTime tokenExpiry;

    public KisAuthService(WebClient.Builder webClientBuilder, StockProperties stockProperties) {
        this.stockProperties = stockProperties;
        this.webClient = webClientBuilder
            .baseUrl(stockProperties.getKis().getBaseUrl())
            .build();
    }

    /**
     * API 키 설정 여부 확인
     */
    public boolean isConfigured() {
        String appKey = stockProperties.getKis().getAppKey();
        String appSecret = stockProperties.getKis().getAppSecret();
        return appKey != null && !appKey.isBlank()
            && appSecret != null && !appSecret.isBlank();
    }

    /**
     * 유효한 접근토큰 반환 (필요시 자동 갱신)
     */
    public String getAccessToken() {
        if (!isConfigured()) {
            log.warn("KIS API keys not configured");
            return null;
        }

        if (isTokenValid()) {
            return accessToken;
        }

        tokenLock.lock();
        try {
            if (isTokenValid()) {
                return accessToken;
            }
            refreshAccessToken();
            return accessToken;
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 토큰 유효성 확인
     */
    private boolean isTokenValid() {
        return accessToken != null
            && tokenExpiry != null
            && LocalDateTime.now().plusMinutes(TOKEN_REFRESH_BUFFER_MINUTES).isBefore(tokenExpiry);
    }

    /**
     * 접근토큰 발급 (oauth2/tokenP) - 재시도 포함
     */
    private void refreshAccessToken() {
        log.info("Refreshing KIS access token");

        Map<String, String> requestBody = Map.of(
            "grant_type", "client_credentials",
            "appkey", stockProperties.getKis().getAppKey(),
            "appsecret", stockProperties.getKis().getAppSecret()
        );

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                KisTokenResponse response = webClient.post()
                    .uri("/oauth2/tokenP")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(KisTokenResponse.class)
                    .timeout(TIMEOUT)
                    .block();

                if (response != null && response.accessToken() != null) {
                    this.accessToken = response.accessToken();
                    this.tokenExpiry = LocalDateTime.now().plusSeconds(response.expiresIn());
                    log.info("KIS access token refreshed, expires at: {}", tokenExpiry);
                    return;
                } else {
                    log.error("Failed to refresh KIS access token: empty response");
                    lastException = new RuntimeException("Empty token response");
                }
            } catch (WebClientResponseException e) {
                log.error("Failed to refresh KIS access token (attempt {}): {} - {}",
                    attempt, e.getStatusCode(), e.getResponseBodyAsString());
                lastException = e;

                // 재시도 불가능한 상태 코드는 즉시 실패
                int status = e.getStatusCode().value();
                if (status >= 400 && status < 500 && status != 429) {
                    throw new RuntimeException("Failed to refresh KIS access token", e);
                }
            } catch (Exception e) {
                log.error("Failed to refresh KIS access token (attempt {}): {}", attempt, e.getMessage());
                lastException = e;

                // 재시도 가능한 예외인지 확인
                if (!isRetryableException(e)) {
                    throw new RuntimeException("Failed to refresh KIS access token", e);
                }
            }

            // 마지막 시도가 아니면 백오프 후 재시도
            if (attempt < MAX_RETRY_ATTEMPTS) {
                long backoffMs = INITIAL_BACKOFF_MS * (1L << (attempt - 1)); // 지수 백오프
                log.warn("Retrying token refresh in {}ms (attempt {}/{})", backoffMs, attempt + 1, MAX_RETRY_ATTEMPTS);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Token refresh interrupted", ie);
                }
            }
        }

        // 모든 재시도 실패
        throw new RuntimeException("Failed to refresh KIS access token after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
    }

    /**
     * 재시도 가능한 예외인지 확인
     */
    private boolean isRetryableException(Exception e) {
        Throwable cause = e.getCause();
        return cause instanceof ConnectException
            || cause instanceof SocketTimeoutException
            || cause instanceof IOException
            || (e.getMessage() != null && e.getMessage().contains("prematurely closed"));
    }

    /**
     * 접근토큰 폐기 (oauth2/revokeP)
     */
    public void revokeToken() {
        if (accessToken == null) {
            return;
        }

        log.info("Revoking KIS access token");

        Map<String, String> requestBody = Map.of(
            "appkey", stockProperties.getKis().getAppKey(),
            "appsecret", stockProperties.getKis().getAppSecret(),
            "token", accessToken
        );

        try {
            webClient.post()
                .uri("/oauth2/revokeP")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(TIMEOUT)
                .block();

            this.accessToken = null;
            this.tokenExpiry = null;
            log.info("KIS access token revoked");
        } catch (Exception e) {
            log.error("Failed to revoke KIS access token: {}", e.getMessage());
        }
    }

    /**
     * Hashkey 생성 (POST 요청 body 암호화용)
     */
    public String generateHashkey(Map<String, Object> requestBody) {
        if (!isConfigured()) {
            return null;
        }

        try {
            KisHashkeyResponse response = webClient.post()
                .uri("/uapi/hashkey")
                .header("appkey", stockProperties.getKis().getAppKey())
                .header("appsecret", stockProperties.getKis().getAppSecret())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(KisHashkeyResponse.class)
                .timeout(TIMEOUT)
                .block();

            return response != null ? response.hashkey() : null;
        } catch (Exception e) {
            log.error("Failed to generate hashkey: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 인증 헤더 생성
     */
    public Map<String, String> buildAuthHeaders(String trId) {
        String token = getAccessToken();
        return Map.of(
            "authorization", "Bearer " + token,
            "appkey", stockProperties.getKis().getAppKey(),
            "appsecret", stockProperties.getKis().getAppSecret(),
            "tr_id", trId,
            "custtype", "P"
        );
    }

    /**
     * 계좌번호 반환
     */
    public String getAccountNumber() {
        return stockProperties.getKis().getAccountNumber();
    }

    /**
     * 계좌상품코드 반환
     */
    public String getAccountProductCode() {
        return stockProperties.getKis().getAccountProductCode();
    }
}
