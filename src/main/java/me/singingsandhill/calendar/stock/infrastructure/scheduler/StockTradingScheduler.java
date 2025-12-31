package me.singingsandhill.calendar.stock.infrastructure.scheduler;

import me.singingsandhill.calendar.stock.application.service.GapPullbackBotService;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 주식 트레이딩 스케줄러
 *
 * 거래 타임라인:
 * 08:30      사전 준비 (전일 데이터 수집)
 * 09:00      갭 상승 종목 스크리닝
 * 09:10~11:20  5초 간격 트레이딩 루프
 * 11:20      최종 청산
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

    /**
     * 사전 준비 (08:30 월~금)
     * - 전일 데이터 수집
     * - 관심종목 풀 준비
     */
    @Scheduled(cron = "0 30 8 * * MON-FRI", zone = "Asia/Seoul")
    public void executePreMarket() {
        if (!isEnabled() || !isTradingDay()) {
            return;
        }

        log.info("Scheduled pre-market execution");
        try {
            botService.executePreMarketLoop();
        } catch (Exception e) {
            log.error("Error in pre-market execution: {}", e.getMessage(), e);
        }
    }

    /**
     * 갭 상승 종목 스크리닝 (09:00 월~금)
     * - 2-7% 갭 상승 종목 필터링
     * - 시가총액, 거래대금, 체결강도 필터
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI", zone = "Asia/Seoul")
    public void executeScreening() {
        if (!isEnabled() || !isTradingDay()) {
            return;
        }

        log.info("Scheduled screening execution");
        try {
            botService.executeScreeningLoop();
        } catch (Exception e) {
            log.error("Error in screening execution: {}", e.getMessage(), e);
        }
    }

    /**
     * 트레이딩 루프 (09:10~11:20 5초 간격)
     * - 리스크 관리 (손절/익절/트레일링)
     * - 상태 머신 업데이트
     * - 진입 실행
     */
    @Scheduled(fixedRateString = "#{${stock.trading.polling-interval-seconds:5} * 1000}",
               initialDelay = 60000)
    public void executeTradingLoop() {
        if (!isEnabled() || !isTradingDay()) {
            return;
        }

        // 거래 시간 체크 (09:10 ~ 11:20)
        LocalTime now = LocalTime.now(KST);
        LocalTime screeningEnd = LocalTime.parse(stockProperties.getTrading().getScreeningEnd());
        LocalTime finalExit = LocalTime.parse(stockProperties.getExit().getFinalExitTime());

        if (now.isBefore(screeningEnd) || now.isAfter(finalExit)) {
            return;
        }

        log.debug("Scheduled trading loop execution");
        try {
            botService.executeTradingLoop();
        } catch (Exception e) {
            log.error("Error in trading loop execution: {}", e.getMessage(), e);
        }
    }

    /**
     * 최종 청산 (11:20 월~금)
     * - 모든 오픈 포지션 청산
     */
    @Scheduled(cron = "0 20 11 * * MON-FRI", zone = "Asia/Seoul")
    public void executeFinalExit() {
        if (!isEnabled() || !isTradingDay()) {
            return;
        }

        log.warn("Scheduled final exit execution");
        try {
            botService.executeFinalExitCheck();
        } catch (Exception e) {
            log.error("Error in final exit execution: {}", e.getMessage(), e);
        }
    }

    /**
     * 봇 활성화 여부 확인
     */
    private boolean isEnabled() {
        return stockProperties.getBot().isEnabled();
    }

    /**
     * 거래일 여부 확인 (주말 제외, 휴일은 별도 구현 필요)
     */
    private boolean isTradingDay() {
        LocalDate today = LocalDate.now(KST);
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        // 주말 제외
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        // TODO: 한국 주식시장 휴일 체크 (공휴일, 임시공휴일 등)
        // 별도 휴일 데이터 관리 또는 외부 API 연동 필요

        return true;
    }
}
