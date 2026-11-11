package me.singingsandhill.calendar.trading.replay;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ReplayStatsTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 8, 1, 0, 0);

    private static ReplayTrade trade(int closedHour, String pnl, double pnlPct, String reason) {
        return new ReplayTrade(T0, T0.plusHours(closedHour), reason, new BigDecimal(pnl), pnlPct);
    }

    @Test
    void maxDrawdownIsTheLargestDropOfCumulativePnlInCloseOrder() {
        // 청산 순서 누적: +10, -20, -15, -25, +15 → 고점 10 에서 -25 까지 35
        List<ReplayTrade> trades = List.of(trade(5, "40", 4, "SIGNAL"), trade(1, "10", 1, "TAKE_PROFIT"),
                trade(3, "5", 0.5, "TIME_EXIT"), trade(2, "-30", -3, "STOP_LOSS"), trade(4, "-10", -1, "STOP_LOSS"));

        ReplayStats stats = ReplayStats.of(trades);

        assertThat(stats.maxDrawdown()).isEqualByComparingTo("35");
        assertThat(stats.total()).isEqualByComparingTo("15");
        assertThat(stats.reasons()).containsEntry("STOP_LOSS", 2).containsEntry("SIGNAL", 1);
    }

    @Test
    void winRateAverageWinLossAndExpectancy() {
        ReplayStats stats = ReplayStats.of(List.of(trade(1, "20", 2, "A"), trade(2, "-10", -1, "B"),
                trade(3, "10", 1, "A"), trade(4, "-20", -2, "B")));

        assertThat(stats.count()).isEqualTo(4);
        assertThat(stats.wins()).isEqualTo(2);
        assertThat(stats.winRatePct()).isCloseTo(50.0, within(1e-9));
        assertThat(stats.avgWinPct()).isCloseTo(1.5, within(1e-9));
        assertThat(stats.avgLossPct()).isCloseTo(-1.5, within(1e-9));
        assertThat(stats.expectancyPct()).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void emptyInputIsAllZero() {
        ReplayStats stats = ReplayStats.of(List.of());

        assertThat(stats.count()).isZero();
        assertThat(stats.total()).isEqualByComparingTo("0");
        assertThat(stats.maxDrawdown()).isEqualByComparingTo("0");
    }
}
