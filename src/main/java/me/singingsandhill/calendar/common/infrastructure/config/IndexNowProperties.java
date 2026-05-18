package me.singingsandhill.calendar.common.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * IndexNow 프로토콜 설정.
 *
 * <p>키 파일은 {@code classpath:static/<key>.txt} 에 두고 같은 호스트로 서빙되어야 한다.
 * 기본 비활성 — {@code INDEXNOW_ENABLED=true} 로 명시적으로 켠다.
 */
@ConfigurationProperties(prefix = "indexnow")
public record IndexNowProperties(
        boolean enabled,
        String key,
        String keyLocation,
        String host,
        String endpoint
) {}
