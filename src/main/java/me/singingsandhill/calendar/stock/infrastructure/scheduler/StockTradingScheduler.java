package me.singingsandhill.calendar.stock.infrastructure.scheduler;

import me.singingsandhill.calendar.stock.application.observability.TradeEvents;
import me.singingsandhill.calendar.stock.application.service.GapPullbackBotService;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 주식 트레이딩 스케줄러.
 *
 * 거래 타임라인:
 *   08:30  pre-market
 *   09:20  스크리닝
 *   09:20~11:20  5초 트레이딩 루프
 *   11:20  최종 청산
 */
@Component
public class StockTradingScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockTradingScheduler.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final GapPullbackBotService botService;
    private final StockProperties stockProperties;

    public StockTradingScheduler(GapPullbackBotService botService,
                                  StockProperties stockProperties) {
        this.botService = botService;
        this.stockProperties = stockProperties;
    }

    @Scheduled(cron = "0 30 8 * * MON-FRI", zone = "Asia/Seoul")
    public void executePreMarket() {
        if (!isEnabled() || !isTradingDay()) {
            return;
        }
        try (Closeable ignored = TradeEvents.phase("PRE_MARKET")) {
            log.info("Scheduled pre-market execution");
            botService.executePreMarketLoop();
        } catch (Exception e) {
            log.error("Error in pre-market execution: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 20 9 * * MON-FRI", zone = "Asia/Seoul")
    public void executeScreening() {
        if (!isEnabled() || !isTradingDay()) {
            return;
        }
        try (Closeable ignored = TradeEvents.phase("SCREENING")) {
            log.info("Scheduled screening execution");
            botService.executeScreeningLoop();
        } catch (Exception e) {
            log.error("Error in screening execution: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRateString = "#{${stock.trading.polling-interval-seconds:5} * 1000}",
               initialDelay = 60000)
    public void executeTradingLoop() {
        if (!isEnabled() || !isTradingDay()) {
            return;
        }

        LocalTime now = LocalTime.now(KST);
        LocalTime tradingLoopStart = LocalTime.parse(stockProperties.getTrading().getTradingLoopStart());
        LocalTime finalExit = LocalTime.parse(stockProperties.getExit().getFinalExitTime());

        if (now.isBefore(tradingLoopStart) || now.isAfter(finalExit)) {
            return;
        }

        try (Closeable ignored = TradeEvents.phase("TRADING")) {
            botService.executeTradingLoop();
        } catch (Exception e) {
            log.error("Error in trading loop execution: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 20 11 * * MON-FRI", zone = "Asia/Seoul")
    public void executeFinalExit() {
        if (!isEnabled() || !isTradingDay()) {
            return;
        }
        try (Closeable ignored = TradeEvents.phase("FINAL_EXIT")) {
            log.warn("Scheduled final exit execution");
            botService.executeFinalExitCheck();
        } catch (Exception e) {
            log.error("Error in final exit execution: {}", e.getMessage(), e);
        }
    }

    private boolean isEnabled() {
        return stockProperties.getBot().isEnabled();
    }

    /**
     * 거래일 여부 (주말 + KRX 휴일 제외).
     */
    private boolean isTradingDay() {
        LocalDate today = LocalDate.now(KST);
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        return !stockProperties.getTrading().isHoliday(today);
    }
}
