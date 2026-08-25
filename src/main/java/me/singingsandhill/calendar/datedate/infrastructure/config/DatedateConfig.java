package me.singingsandhill.calendar.datedate.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import me.singingsandhill.calendar.datedate.domain.owner.OwnerIdGenerator;

@Configuration
public class DatedateConfig {

    /**
     * 랜덤 owner ID 생성기. 도메인 클래스를 Spring 무관하게 두기 위해 여기서 빈으로 올린다
     * (stock 모듈이 {@code Clock} 을 StockSchedulerConfig 에서 올리는 것과 같은 방식).
     * 테스트는 시드 고정 {@code RandomGenerator} 를 직접 주입한다.
     */
    @Bean
    public OwnerIdGenerator ownerIdGenerator() {
        return new OwnerIdGenerator();
    }
}
