package me.singingsandhill.calendar.trading.infrastructure.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class TradingSchedulerConfig {
    // 스케줄링 활성화 설정
    // 실제 스케줄 작업은 CandleScheduler, DailySummaryScheduler에서 정의
}
