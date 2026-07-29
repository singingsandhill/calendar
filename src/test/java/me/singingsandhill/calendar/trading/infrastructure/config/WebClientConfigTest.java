package me.singingsandhill.calendar.trading.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.resources.ConnectionProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 커넥션 풀 정책 구성 스모크 (ADR trading/infrastructure/0004).
 *
 * ConnectionProvider 는 maxIdleTime 등 설정값 introspection API 가 없어 수치를 단정할 수
 * 없다 — 여기서는 커스텀 풀(명명 "trading-stock-shared")을 낀 빌더가 정상 구성되는 것만
 * 고정하고, stale 커넥션 재사용(PrematureCloseException) 소멸 여부는 운영 로그로 검증한다.
 */
class WebClientConfigTest {

    @Test
    void webClientBuilder_buildsWithPooledConnectionProvider() {
        WebClientConfig config = new WebClientConfig();
        ConnectionProvider provider = config.tradingConnectionProvider();
        try {
            assertThat(provider.name()).isEqualTo("trading-stock-shared");

            WebClient.Builder builder = config.webClientBuilder(provider);
            assertThat(builder).isNotNull();
            assertThat(builder.build()).isNotNull();
        } finally {
            provider.dispose();
        }
    }
}
