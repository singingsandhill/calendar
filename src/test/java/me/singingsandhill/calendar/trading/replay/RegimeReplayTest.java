package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.domain.regime.DailyBar;
import me.singingsandhill.calendar.trading.domain.regime.MarketRegime;
import me.singingsandhill.calendar.trading.domain.regime.RegimeCalculator;
import me.singingsandhill.calendar.trading.domain.regime.RegimePoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 레짐 필터 평가(작업 6-B) — 진입일 D 의 레짐은 D-1 종가까지의 판정(운영과 같은 RegimeCalculator), 미래 데이터 금지.
 * 종가 100,100,100 (MA3 = 100 → BULL) → 90 (BEAR) → 110 (BULL).
 */
class RegimeReplayTest {

    private static final LocalDate D1 = LocalDate.of(2026, 1, 1);

    private static List<RegimePoint> points() {
        List<DailyBar> bars = new ArrayList<>();
        String[] closes = {"100", "100", "100", "90", "110"};
        for (int i = 0; i < closes.length; i++) {
            BigDecimal c = new BigDecimal(closes[i]);
            bars.add(new DailyBar(D1.plusDays(i), c, c, c, c));
        }
        return RegimeCalculator.compute(bars, 3, 0.0, 1);
    }

    private static ReplayTrade openedOn(int dayIndex, String pnl) {
        LocalDateTime opened = D1.plusDays(dayIndex).atTime(10, 0);
        return new ReplayTrade(opened, opened.plusHours(1), "SIGNAL", new BigDecimal(pnl), Double.parseDouble(pnl) / 100);
    }

    @Test
    void entryUsesThePreviousDaysVerdictOnly() {
        List<RegimePoint> points = points();

        assertThat(RegimeReplay.regimeAtEntry(points, openedOn(2, "0"))).isEqualTo(MarketRegime.UNKNOWN); // 1/2 판정 = MA 부족
        assertThat(RegimeReplay.regimeAtEntry(points, openedOn(3, "0"))).isEqualTo(MarketRegime.BULL);    // 1/3 판정
        assertThat(RegimeReplay.regimeAtEntry(points, openedOn(4, "0"))).isEqualTo(MarketRegime.BEAR);    // 1/4 종가 90 — 당일 110 은 안 봄
        assertThat(RegimeReplay.regimeAtEntry(points, openedOn(5, "0"))).isEqualTo(MarketRegime.BULL);
    }

    @Test
    void variantsBlockOrScaleBearEntriesAndFollowTheUnknownPolicy() {
        List<ReplayTrade> trades = List.of(openedOn(2, "5"), openedOn(3, "10"), openedOn(4, "-20"), openedOn(5, "30"));
        List<RegimePoint> points = points();

        assertThat(total(RegimeReplay.apply(RegimeReplay.Variant.OFF, 0.5, points, trades))).isEqualByComparingTo("25");
        assertThat(total(RegimeReplay.apply(RegimeReplay.Variant.BLOCK_BEAR, 0.5, points, trades))).isEqualByComparingTo("40");
        assertThat(total(RegimeReplay.apply(RegimeReplay.Variant.BLOCK_BEAR_ALLOW_UNKNOWN, 0.5, points, trades)))
                .isEqualByComparingTo("45");
        assertThat(total(RegimeReplay.apply(RegimeReplay.Variant.SCALE_BEAR, 0.5, points, trades))).isEqualByComparingTo("30");
        assertThat(total(RegimeReplay.apply(RegimeReplay.Variant.SCALE_BEAR_ALLOW_UNKNOWN, 0.5, points, trades)))
                .isEqualByComparingTo("35");
    }

    @Test
    void filterQualifiesOnlyWhenBothPeriodsImproveWithBearEntriesWorse() {
        List<RegimePoint> points = points();
        // openedOn(3) = 1/4 진입(1/3 판정 BULL), openedOn(4) = 1/5 진입(1/4 판정 BEAR)
        List<ReplayTrade> first = List.of(openedOn(3, "10"), openedOn(4, "-20"));
        List<ReplayTrade> second = List.of(openedOn(3, "5"), openedOn(4, "-1"));
        List<ReplayTrade> bearBetterLater = List.of(openedOn(3, "5"), openedOn(4, "30"));

        assertThat(RegimeReplay.improvesBothPeriods(RegimeReplay.Variant.BLOCK_BEAR, 0.5, points, first, second)).isTrue();
        assertThat(RegimeReplay.improvesBothPeriods(RegimeReplay.Variant.SCALE_BEAR, 0.5, points, first, second)).isTrue();
        assertThat(RegimeReplay.improvesBothPeriods(RegimeReplay.Variant.BLOCK_BEAR, 0.5, points, first, bearBetterLater))
                .isFalse();
    }

    private static BigDecimal total(List<ReplayTrade> trades) {
        return trades.stream().map(ReplayTrade::pnl).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
