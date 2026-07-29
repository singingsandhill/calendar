package me.singingsandhill.calendar.trading.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_SEC = 30;
    private static final int WRITE_TIMEOUT_SEC = 30;

    // ADR trading/infrastructure/0004: 기본 provider 는 idle 커넥션을 폐기하지 않아
    // 서버(Bithumb)가 keep-alive 를 닫은 stale 커넥션 재사용 → PrematureCloseException.
    // 서버측 idle 타임아웃보다 보수적으로 짧게 잡아 재사용 전에 우리가 먼저 버린다.
    // 이 빌더는 싱글턴이라 stock 모듈(KIS)도 같은 풀을 공유한다.
    private static final Duration POOL_MAX_IDLE = Duration.ofSeconds(10);
    private static final Duration POOL_MAX_LIFE = Duration.ofMinutes(5);
    private static final Duration POOL_EVICT_INTERVAL = Duration.ofSeconds(30);

    @Bean(destroyMethod = "dispose")
    public ConnectionProvider tradingConnectionProvider() {
        return ConnectionProvider.builder("trading-stock-shared")
                .maxIdleTime(POOL_MAX_IDLE)
                .maxLifeTime(POOL_MAX_LIFE)
                .evictInBackground(POOL_EVICT_INTERVAL)
                .build();
    }

    @Bean
    public WebClient.Builder webClientBuilder(ConnectionProvider tradingConnectionProvider) {
        HttpClient httpClient = HttpClient.create(tradingConnectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(READ_TIMEOUT_SEC))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SEC, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
