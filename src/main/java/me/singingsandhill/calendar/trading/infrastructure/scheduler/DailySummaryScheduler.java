package me.singingsandhill.calendar.trading.infrastructure.scheduler;

import me.singingsandhill.calendar.trading.application.service.ProfitService;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DailySummaryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailySummaryScheduler.class);

    private final ProfitService profitService;
    private final TradingProperties tradingProperties;

    public DailySummaryScheduler(ProfitService profitService,
                                  TradingProperties tradingProperties) {
        this.profitService = profitService;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 매 5분마다 계좌 스냅샷 저장
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void saveAccountSnapshot() {
        if (!tradingProperties.getBot().isEnabled()) {
            return;
        }

        log.debug("Saving account snapshot");
        try {
            profitService.saveAccountSnapshot();
        } catch (Exception e) {
            log.error("Failed to save account snapshot", e);
        }
    }

    /**
     * 매일 자정 1분에 전일 일별 요약 생성
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void generateDailySummary() {
        if (!tradingProperties.getBot().isEnabled()) {
            return;
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Generating daily summary for {}", yesterday);

        try {
            profitService.generateDailySummary(yesterday);
        } catch (Exception e) {
            log.error("Failed to generate daily summary for {}", yesterday, e);
        }
    }

    /**
     * 매시간 정각에 계좌 스냅샷 저장 (백업)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void saveHourlySnapshot() {
        if (!tradingProperties.getBot().isEnabled()) {
            return;
        }

        log.debug("Saving hourly account snapshot");
        try {
            profitService.saveAccountSnapshot();
        } catch (Exception e) {
            log.error("Failed to save hourly account snapshot", e);
        }
    }
}
