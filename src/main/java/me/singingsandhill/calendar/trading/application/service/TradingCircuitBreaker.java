package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * P0-2: 트레이딩 서킷브레이커.
 * 연속 손실 횟수 또는 일일 실현손실이 임계를 넘으면 신규 진입(BUY)을 차단한다.
 * 리스크 청산(손절/익절/트레일링)은 차단하지 않는다 — 자본 보호는 계속.
 */
@Component
public class TradingCircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(TradingCircuitBreaker.class);

    private final TradingProperties tradingProperties;

    /** 연속 손실 스트릭. 손실 청산마다 +1, 이익 청산 시 0 으로 리셋. */
    private volatile int consecutiveLosses = 0;

    public TradingCircuitBreaker(TradingProperties tradingProperties) {
        this.tradingProperties = tradingProperties;
    }

    /**
     * 포지션 청산 결과 기록. 손실이면 스트릭 증가, 이익/본전이면 리셋.
     */
    public void recordOutcome(BigDecimal realizedPnl) {
        if (realizedPnl != null && realizedPnl.signum() < 0) {
            consecutiveLosses++;
            log.debug("Circuit breaker: consecutive losses = {}", consecutiveLosses);
        } else {
            consecutiveLosses = 0;
        }
    }

    /**
     * 신규 진입(BUY) 차단 여부.
     * @param dayStartEquity   당일 시작 자본 (KRW). null/0 이면 일일 손실 가드 스킵.
     * @param realizedPnlToday 당일 실현손익 (KRW, 손실이면 음수).
     */
    public boolean isEntryBlocked(BigDecimal dayStartEquity, BigDecimal realizedPnlToday) {
        if (!tradingProperties.getRisk().isCircuitBreakerEnabled()) {
            return false;
        }

        int maxConsecutive = tradingProperties.getRisk().getMaxConsecutiveLosses();
        if (consecutiveLosses >= maxConsecutive) {
            log.warn("Circuit breaker tripped: {} consecutive losses >= {} — blocking new entries",
                    consecutiveLosses, maxConsecutive);
            return true;
        }

        if (dayStartEquity != null && dayStartEquity.signum() > 0 && realizedPnlToday != null) {
            BigDecimal lossPct = realizedPnlToday.divide(dayStartEquity, 6, RoundingMode.HALF_UP);
            BigDecimal limit = BigDecimal.valueOf(tradingProperties.getRisk().getMaxDailyLossPct());
            if (lossPct.compareTo(limit) <= 0) {
                log.warn("Circuit breaker tripped: daily PnL {}% <= {}% limit — blocking new entries",
                        lossPct.multiply(BigDecimal.valueOf(100)),
                        limit.multiply(BigDecimal.valueOf(100)));
                return true;
            }
        }

        return false;
    }

    public int getConsecutiveLosses() {
        return consecutiveLosses;
    }
}
