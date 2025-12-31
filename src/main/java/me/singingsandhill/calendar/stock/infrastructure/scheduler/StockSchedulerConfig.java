package me.singingsandhill.calendar.stock.infrastructure.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 주식 트레이딩 스케줄링 설정
 * 실제 스케줄 작업은 StockTradingScheduler에서 정의
 */
@Configuration
@EnableScheduling
public class StockSchedulerConfig {
}
