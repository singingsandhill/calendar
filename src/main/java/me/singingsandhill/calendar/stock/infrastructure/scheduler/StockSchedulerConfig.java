package me.singingsandhill.calendar.stock.infrastructure.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 주식 트레이딩 스케줄링 설정.
 *
 * PR-5: 단일 스레드 스케줄러를 4 스레드 풀로 대체하여
 * 09:20 스크리닝(20초+)이 트레이딩 루프(5초)와 병렬로 실행되도록 한다.
 */
@Configuration
@EnableScheduling
public class StockSchedulerConfig {

    /**
     * 트레이딩 시각 의존성을 외부화하기 위한 Clock 빈.
     * 테스트에서 {@code Clock.fixed(...)} 으로 결정성 확보.
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock stockClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("stock-sched-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        return scheduler;
    }
}
