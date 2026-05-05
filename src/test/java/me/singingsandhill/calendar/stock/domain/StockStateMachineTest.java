package me.singingsandhill.calendar.stock.domain;

import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stock 상태 머신 단위 테스트.
 *
 * 보호 시나리오:
 *  - WATCHING -> HIGH_FORMED: currentPrice >= openPrice * (1 + threshold/100)
 *  - HIGH_FORMED -> PULLBACK: drop in [-pullbackMin, -pullbackMax]
 *  - PULLBACK -> ENTRY_READY: bounce >= bounceThreshold
 *  - PULLBACK -> FILTERED_OUT: drop < -pullbackMax (과도 하락)
 */
class StockStateMachineTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 1);
    private static final BigDecimal HIGH_THRESHOLD = new BigDecimal("1.5"); // +1.5%
    private static final BigDecimal PULLBACK_MIN = new BigDecimal("1.5");
    private static final BigDecimal PULLBACK_MAX = new BigDecimal("3.0");
    private static final BigDecimal BOUNCE_THRESHOLD = new BigDecimal("0.3");

    private Stock stock(BigDecimal open) {
        Stock s = new Stock("005930", "삼성전자", TODAY);
        s.setOpenPrice(open);
        s.setCurrentPrice(open);
        return s;
    }

    @Test
    void watchingToHighFormed_whenPriceCrossesThreshold() {
        Stock s = stock(new BigDecimal("100000"));
        // +1.6% = 101600 (>= +1.5% threshold)
        s.updateCurrentPrice(new BigDecimal("101600"));

        assertThat(s.isHighFormed(HIGH_THRESHOLD)).isTrue();
        s.recordHighFormed(s.getCurrentPrice());
        assertThat(s.getState()).isEqualTo(StockState.HIGH_FORMED);
    }

    @Test
    void watchingStays_whenBelowThreshold() {
        Stock s = stock(new BigDecimal("100000"));
        // +1.0% < +1.5%
        s.updateCurrentPrice(new BigDecimal("101000"));
        assertThat(s.isHighFormed(HIGH_THRESHOLD)).isFalse();
        assertThat(s.getState()).isEqualTo(StockState.WATCHING);
    }

    @Test
    void highFormedToPullback_whenInPullbackRange() {
        Stock s = stock(new BigDecimal("100000"));
        s.updateCurrentPrice(new BigDecimal("102000")); // +2%
        s.recordHighFormed(s.getCurrentPrice());

        // 102000 -> 100000 = -1.96% (in [1.5, 3.0])
        s.updateCurrentPrice(new BigDecimal("100000"));
        assertThat(s.isInPullbackRange(PULLBACK_MIN, PULLBACK_MAX)).isTrue();
    }

    @Test
    void pullbackTooDeep_whenDropExceedsMax() {
        Stock s = stock(new BigDecimal("100000"));
        s.updateCurrentPrice(new BigDecimal("102000"));
        s.recordHighFormed(s.getCurrentPrice());
        // 102000 -> 98000 = -3.92% < -3%
        s.updateCurrentPrice(new BigDecimal("98000"));
        assertThat(s.isPullbackTooDeep(PULLBACK_MAX)).isTrue();
    }

    @Test
    void bounceConfirmed_whenAboveBounceThreshold() {
        Stock s = stock(new BigDecimal("100000"));
        s.updateCurrentPrice(new BigDecimal("102000"));
        s.recordHighFormed(s.getCurrentPrice());
        s.recordPullbackStart(new BigDecimal("100000"));
        // pullbackLow=100000, current 100400 -> +0.4%
        s.updateCurrentPrice(new BigDecimal("100400"));
        assertThat(s.isBounceConfirmed(BOUNCE_THRESHOLD)).isTrue();
    }

    @Test
    void bounceNotConfirmed_whenBelowBounceThreshold() {
        Stock s = stock(new BigDecimal("100000"));
        s.updateCurrentPrice(new BigDecimal("102000"));
        s.recordHighFormed(s.getCurrentPrice());
        s.recordPullbackStart(new BigDecimal("100000"));
        // +0.1% < +0.3%
        s.updateCurrentPrice(new BigDecimal("100100"));
        assertThat(s.isBounceConfirmed(BOUNCE_THRESHOLD)).isFalse();
    }
}
