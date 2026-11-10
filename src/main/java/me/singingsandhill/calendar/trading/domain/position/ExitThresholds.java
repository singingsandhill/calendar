package me.singingsandhill.calendar.trading.domain.position;

import me.singingsandhill.calendar.trading.domain.trade.BithumbTickSize;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * 출구 임계값의 틱 하한 (ADR trading/strategy/0011) — 호가단위 몇 틱보다 좁은 손절·트레일링은 노이즈에 걸린다.
 *
 * 최적화가 아니라 하한이다: 설정값이 더 넓으면 그대로 쓴다. ADA ≈ 290원이면 1틱 = 0.34% 라 트레일링 0.8% 는 2.4틱,
 * 손절 1.5% 는 4.4틱이었다. 가격은 가격 공간에서 계산해 호가 격자로 내린다 — 비율을 곱하면 부동소수 오차로 정확히
 * 격자 위에 떨어져야 할 값이 한 틱 아래로 내려갈 수 있다. 리스크 루프와 리플레이(작업 6-A)가 같은 계산을 쓴다.
 */
public final class ExitThresholds {

    private ExitThresholds() {
    }

    /** 유효 손절(음수 비율) = min(stopLoss, -minTicks × 틱 / 기준가). 기준가 = 진입가. 틱 0 이거나 기준가가 없으면 그대로. */
    public static double effectiveStopLoss(double stopLoss, int minTicks, BigDecimal referencePrice) {
        if (minTicks <= 0 || referencePrice == null || referencePrice.signum() <= 0) {
            return stopLoss;
        }
        double floor = tickDistance(referencePrice, minTicks).divide(referencePrice, MathContext.DECIMAL64).doubleValue();
        return Math.min(stopLoss, -floor);
    }

    /** 손절가 = 진입가 - max(진입가 × |stopLoss|, minTicks 틱), 호가 격자로 내림. */
    public static BigDecimal stopLossPrice(BigDecimal entryPrice, double stopLoss, int minTicks) {
        return below(entryPrice, Math.abs(stopLoss), minTicks);
    }

    /** 트레일링 스탑 가격 = 기준가(HWM) - max(기준가 × trailing, minTicks 틱), 호가 격자로 내림. */
    public static BigDecimal trailingStopPrice(BigDecimal highWaterMark, double trailing, int minTicks) {
        return below(highWaterMark, trailing, minTicks);
    }

    private static BigDecimal below(BigDecimal price, double ratio, int minTicks) {
        BigDecimal distance = price.multiply(BigDecimal.valueOf(ratio));
        if (minTicks > 0) {
            distance = distance.max(tickDistance(price, minTicks));
        }
        return BithumbTickSize.roundDown(price.subtract(distance));
    }

    private static BigDecimal tickDistance(BigDecimal price, int minTicks) {
        return BithumbTickSize.tickSize(price).multiply(BigDecimal.valueOf(minTicks));
    }
}
