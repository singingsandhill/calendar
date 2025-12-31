package me.singingsandhill.calendar.trading.infrastructure.scheduler;

import me.singingsandhill.calendar.trading.application.service.CandleService;
import me.singingsandhill.calendar.trading.application.service.TradingBotService;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CandleScheduler {

    private static final Logger log = LoggerFactory.getLogger(CandleScheduler.class);

    private final TradingBotService tradingBotService;
    private final CandleService candleService;
    private final TradingProperties tradingProperties;

    public CandleScheduler(TradingBotService tradingBotService,
                           CandleService candleService,
                           TradingProperties tradingProperties) {
        this.tradingBotService = tradingBotService;
        this.candleService = candleService;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 매분 5초에 실행 (캔들 완성 후 실행)
     * 1분 캔들이 0초에 확정되므로 5초 여유를 두고 실행
     */
    @Scheduled(cron = "5 * * * * *")
    public void executeTradeLoop() {
        if (!tradingProperties.getBot().isEnabled()) {
            return;
        }

        log.debug("Scheduled trade loop execution");
        tradingBotService.executeTradeLoop();
    }

    /**
     * 매일 자정에 오래된 캔들 데이터 정리
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldCandles() {
        log.info("Scheduled cleanup of old candles");
        int deleted = candleService.cleanupOldCandles();
        log.info("Cleanup completed: {} candles deleted", deleted);
    }

    /**
     * 매 5분마다 캔들 데이터 동기화 (누락분 보완)
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void syncCandles() {
        if (!tradingProperties.getBot().isEnabled()) {
            return;
        }

        log.debug("Syncing candle data");
        int saved = candleService.fetchAndSaveCandles();
        if (saved > 0) {
            log.info("Synced {} missing candles", saved);
        }
    }
}
