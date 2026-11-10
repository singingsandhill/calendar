package me.singingsandhill.calendar.trading.domain.position;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * 틱 하한 — 노이즈(호가단위 몇 틱)보다 좁은 손절·트레일링 금지 (ADR trading/strategy/0011).
 *
 * 최적화가 아니라 하한이다: 유효 손절 = min(stop%, -N틱/진입가), 트레일링 폭 = max(HWM × trailing%, N틱).
 * 290원·5틱이면 손절 -1.5% → -1.724%, 트레일링 0.8%(2.32원) → 5원. 1,000원이면 5틱 = 0.5% 라 종전 값 그대로.
 * 가격은 가격 공간에서 계산해 호가 격자로 내린다 — 비율을 곱하면 부동소수 오차로 한 틱(0.34%) 밀릴 수 있다.
 */
class ExitThresholdsTest {

    private static final BigDecimal P290 = new BigDecimal("290");

    @Test
    void stopIsWidenedToTickFloorAtLowPrices() {
        assertThat(ExitThresholds.effectiveStopLoss(-0.015, 5, P290)).isCloseTo(-5.0 / 290, within(1e-9));
    }

    @Test
    void stopFloorDoesNotBindWhenConfiguredStopIsWider() {
        assertThat(ExitThresholds.effectiveStopLoss(-0.015, 5, new BigDecimal("1000"))).isEqualTo(-0.015);
    }

    @Test
    void zeroTicksOrMissingReferenceKeepsConfiguredStop() {
        assertThat(ExitThresholds.effectiveStopLoss(-0.015, 0, P290)).isEqualTo(-0.015);
        assertThat(ExitThresholds.effectiveStopLoss(-0.015, 5, null)).isEqualTo(-0.015);
        assertThat(ExitThresholds.effectiveStopLoss(-0.015, 5, BigDecimal.ZERO)).isEqualTo(-0.015);
    }

    @Test
    void trailingStopHangsAtLeastNTicksBelowTheHighWaterMark() {
        // 300 × 0.8% = 2.4원 < 5틱 → 295 (종전 297)
        assertThat(ExitThresholds.trailingStopPrice(new BigDecimal("300"), 0.008, 5)).isEqualByComparingTo("295");
        // 1,050 × 0.8% = 8.4원 > 5틱 → 설정값 그대로 1,041.6 → 격자 내림 1,041
        assertThat(ExitThresholds.trailingStopPrice(new BigDecimal("1050"), 0.008, 5)).isEqualByComparingTo("1041");
        assertThat(ExitThresholds.trailingStopPrice(new BigDecimal("300"), 0.008, 0)).isEqualByComparingTo("297");
    }

    @Test
    void stopLossPriceUsesTheSameFloorInPriceSpace() {
        // 200 × 1.5% = 3원 < 5틱 → 195 (종전 197)
        assertThat(ExitThresholds.stopLossPrice(new BigDecimal("200"), -0.015, 5)).isEqualByComparingTo("195");
        assertThat(ExitThresholds.stopLossPrice(new BigDecimal("200"), -0.015, 0)).isEqualByComparingTo("197");
        assertThat(ExitThresholds.stopLossPrice(new BigDecimal("1000"), -0.015, 5)).isEqualByComparingTo("985");
    }

    @Test
    void pricesFollowTheTickGridBelowOneHundredWon() {
        // 72.55원(틱 0.01): 0.8% = 0.5804원 > 5틱 0.05원 → 71.9696 → 71.96 (정수 내림이면 71 — 한 칸이 1.3%)
        assertThat(ExitThresholds.trailingStopPrice(new BigDecimal("72.55"), 0.008, 5)).isEqualByComparingTo("71.96");
    }
}
