package me.singingsandhill.calendar.common.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(IndexNowProperties.class)
public class IndexNowConfig {

    @Bean
    public RestClient indexNowRestClient() {
        return RestClient.builder()
                .requestFactory(timeoutClientHttpRequestFactory())
                .build();
    }

    private static org.springframework.http.client.ClientHttpRequestFactory timeoutClientHttpRequestFactory() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
        return factory;
    }
}
