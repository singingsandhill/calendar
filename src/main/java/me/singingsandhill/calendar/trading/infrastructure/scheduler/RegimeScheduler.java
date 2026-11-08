package me.singingsandhill.calendar.trading.infrastructure.scheduler;

import me.singingsandhill.calendar.trading.application.service.DailyRegimeService;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 일봉 레짐 하루 1회 갱신 (ADR trading/risk/0007) — KST 00:05, 전날 일봉 확정 여유 5분.
 * 이 잡이 없어도 레짐 조회 시 00:05 이후 첫 호출이 갱신하지만, 매매가 없는 날에도 REGIME_CHANGED 이벤트가
 * 제때 남도록 미리 당긴다. 다른 트레이딩 잡처럼 bot.enabled=false 면 건너뛴다.
 */
@Component
public class RegimeScheduler {

    private static final Logger log = LoggerFactory.getLogger(RegimeScheduler.class);

    private final DailyRegimeService dailyRegimeService;
    private final TradingProperties tradingProperties;

    public RegimeScheduler(DailyRegimeService dailyRegimeService, TradingProperties tradingProperties) {
        this.dailyRegimeService = dailyRegimeService;
        this.tradingProperties = tradingProperties;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void refreshDailyRegime() {
        if (!tradingProperties.getBot().isEnabled()) {
            return;
        }
        try {
            dailyRegimeService.refresh();
        } catch (Exception e) {
            log.error("Daily regime refresh failed", e);
        }
    }
}
