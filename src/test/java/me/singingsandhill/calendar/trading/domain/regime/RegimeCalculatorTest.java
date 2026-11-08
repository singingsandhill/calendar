package me.singingsandhill.calendar.trading.domain.regime;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 일봉 레짐 판정 (ADR trading/risk/0007) — 종가 vs 단순이동평균, 히스테리시스 밴드, 최소 유지 일수.
 *
 * 리밸런싱의 1분봉 MA60(=60분 평균) 레짐은 10/11~10/20 목표 비중을 70%↔30% 로 매번 뒤집었다. 일봉 레짐은 같은
 * 함수가 운영(DailyRegimeService)과 리플레이(작업 6-B·6-C)에서 쓰여야 결과가 같다.
 */
class RegimeCalculatorTest {

    private static final LocalDate D0 = LocalDate.of(2026, 1, 1);

    private static List<DailyBar> bars(double... closes) {
        List<DailyBar> list = new ArrayList<>();
        for (int i = 0; i < closes.length; i++) {
            BigDecimal c = BigDecimal.valueOf(closes[i]);
            list.add(new DailyBar(D0.plusDays(i), c, c, c, c));
        }
        return list;
    }

    private static MarketRegime last(List<RegimePoint> points) {
        return points.get(points.size() - 1).regime();
    }

    @Test
    void unknownUntilMovingAverageIsAvailable() {
        List<RegimePoint> points = RegimeCalculator.compute(bars(100, 101, 102), 5, 0.0, 1);

        assertThat(points).extracting(RegimePoint::regime)
            .containsOnly(MarketRegime.UNKNOWN);
        assertThat(points.get(2).movingAverage()).isNull();
    }

    @Test
    void firstStateIsCloseVersusAverageWithoutBand() {
        // MA3 of (100,100,103) = 101 → 종가 103 ≥ 101 → BULL
        List<RegimePoint> points = RegimeCalculator.compute(bars(100, 100, 103), 3, 0.05, 3);

        assertThat(last(points)).isEqualTo(MarketRegime.BULL);
        assertThat(points.get(2).movingAverage()).isEqualByComparingTo("101");
    }

    @Test
    void hysteresisBandHoldsStateUntilCloseLeavesIt() {
        // BULL 로 시작 → 종가가 MA 아래로 1%만 내려가면(밴드 2% 안) BULL 유지, 2% 넘게 내려가야 BEAR
        List<RegimePoint> inBand = RegimeCalculator.compute(bars(100, 100, 100, 101, 99.5), 3, 0.02, 1);
        assertThat(last(inBand)).isEqualTo(MarketRegime.BULL);

        List<RegimePoint> outOfBand = RegimeCalculator.compute(bars(100, 100, 100, 101, 95), 3, 0.02, 1);
        assertThat(last(outOfBand)).isEqualTo(MarketRegime.BEAR);
    }

    @Test
    void minDaysPreventsFlipBackTooSoon() {
        // day2: BULL(초기), day3: 급락 → BEAR(유지 1일 ≥ 1 이므로 전환 가능, minDays=2 면 불가)
        double[] closes = {100, 100, 100, 80, 120, 120};
        List<RegimePoint> minDays1 = RegimeCalculator.compute(bars(closes), 3, 0.0, 1);
        List<RegimePoint> minDays2 = RegimeCalculator.compute(bars(closes), 3, 0.0, 2);

        // minDays=1: day3 BEAR, day4 다시 BULL
        assertThat(minDays1.get(3).regime()).isEqualTo(MarketRegime.BEAR);
        assertThat(minDays1.get(4).regime()).isEqualTo(MarketRegime.BULL);
        // minDays=2: day2 초기 BULL 이 하루뿐이라 day3 에 못 바뀌고, day4 는 BULL 신호라 그대로
        assertThat(minDays2.get(3).regime()).isEqualTo(MarketRegime.BULL);
        assertThat(minDays2.get(4).regime()).isEqualTo(MarketRegime.BULL);
    }

    @Test
    void stateSwitchWaitsForMinDaysThenFollowsSignal() {
        // MA3, band 0: BULL(초기, d2) → d3 BEAR 신호(유지 1일 < 3 → 보류) → d4 BEAR 신호(유지 2일 → 보류)
        // → d5 BEAR 신호(유지 3일 → 전환)
        double[] closes = {100, 100, 100, 90, 85, 80};
        List<RegimePoint> points = RegimeCalculator.compute(bars(closes), 3, 0.0, 3);

        assertThat(points.get(3).regime()).isEqualTo(MarketRegime.BULL);
        assertThat(points.get(4).regime()).isEqualTo(MarketRegime.BULL);
        assertThat(points.get(5).regime()).isEqualTo(MarketRegime.BEAR);
    }

    @Test
    void regimeInEffectOnDayUsesPreviousCloseOnly() {
        // 진입일 D 에 적용되는 레짐은 D-1 종가까지의 판정 — 당일(형성 중) 봉은 보지 않는다.
        List<RegimePoint> points = RegimeCalculator.compute(bars(100, 100, 103, 60), 3, 0.0, 1);

        assertThat(RegimeCalculator.regimeInEffectOn(points, D0.plusDays(3))).isEqualTo(MarketRegime.BULL);
        assertThat(RegimeCalculator.regimeInEffectOn(points, D0.plusDays(4))).isEqualTo(MarketRegime.BEAR);
        assertThat(RegimeCalculator.regimeInEffectOn(points, D0.plusDays(30))).isEqualTo(MarketRegime.UNKNOWN);
    }
}
