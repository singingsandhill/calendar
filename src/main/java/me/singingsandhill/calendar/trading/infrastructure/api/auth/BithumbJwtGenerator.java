package me.singingsandhill.calendar.trading.infrastructure.api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import jakarta.annotation.PostConstruct;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class BithumbJwtGenerator {

    private static final Logger log = LoggerFactory.getLogger(BithumbJwtGenerator.class);

    private final String accessKey;
    private final String secretKey;
    private final boolean configured;

    public BithumbJwtGenerator(TradingProperties tradingProperties) {
        this.accessKey = tradingProperties.getBithumb().getAccessKey();
        this.secretKey = tradingProperties.getBithumb().getSecretKey();
        this.configured = isNotEmpty(accessKey) && isNotEmpty(secretKey);
    }

    @PostConstruct
    public void validateConfiguration() {
        if (!configured) {
            log.warn("Bithumb API keys not configured. Set BITHUMB_ACCESS_KEY and BITHUMB_SECRET_KEY environment variables.");
        } else {
            log.info("Bithumb API keys configured successfully");
        }
    }

    public boolean isConfigured() {
        return configured;
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 파라미터가 없는 요청에 대한 JWT 토큰 생성
     */
    public String generateToken() {
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("timestamp", System.currentTimeMillis())
                .sign(algorithm);
    }

    /**
     * 파라미터가 있는 요청에 대한 JWT 토큰 생성 (query_hash 포함)
     */
    public String generateTokenWithParams(Map<String, Object> params) {
        String queryString = buildQueryString(params);
        String queryHash = sha512(queryString);

        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("timestamp", System.currentTimeMillis())
                .withClaim("query_hash", queryHash)
                .withClaim("query_hash_alg", "SHA512")
                .sign(algorithm);
    }

    /**
     * Authorization 헤더 값 생성 ("Bearer " 접두사 포함)
     */
    public String generateAuthorizationHeader() {
        return "Bearer " + generateToken();
    }

    /**
     * Authorization 헤더 값 생성 (파라미터 포함)
     */
    public String generateAuthorizationHeader(Map<String, Object> params) {
        return "Bearer " + generateTokenWithParams(params);
    }

    public String getAccessKey() {
        return accessKey;
    }

    private String buildQueryString(Map<String, Object> params) {
        return params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + String.valueOf(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String sha512(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(input.getBytes(StandardCharsets.UTF_8));
            return String.format("%0128x", new BigInteger(1, md.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512 algorithm not available", e);
        }
    }
}
