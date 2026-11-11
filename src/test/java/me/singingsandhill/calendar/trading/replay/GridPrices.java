package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.domain.trade.BithumbTickSize;

import java.math.BigDecimal;
import java.util.function.Predicate;

/** 호가 격자 위 체결가 탐색 — 판정 손익률이 임계를 넘는 첫 호가(손절·트레일링은 아래로, 익절은 위로). */
final class GridPrices {

    private static final BigDecimal EPSILON = new BigDecimal("0.00000001");

    private GridPrices() {
    }

    /** start 이하 호가 중 hit 을 만족하는 가장 높은 값. 판정 손익률은 가격에 단조라 아래로 걸어 내려가면 된다. */
    static BigDecimal highestAtOrBelow(BigDecimal start, Predicate<BigDecimal> hit) {
        BigDecimal price = BithumbTickSize.roundDown(start);
        while (!hit.test(price) && price.signum() > 0) {
            price = price.subtract(BithumbTickSize.tickSize(price.subtract(EPSILON)));
        }
        return price;
    }

    /** start 이상 호가 중 hit 을 만족하는 가장 낮은 값. */
    static BigDecimal lowestAtOrAbove(BigDecimal start, Predicate<BigDecimal> hit) {
        BigDecimal price = BithumbTickSize.roundUp(start);
        while (!hit.test(price)) {
            price = price.add(BithumbTickSize.tickSize(price));
        }
        return price;
    }
}
