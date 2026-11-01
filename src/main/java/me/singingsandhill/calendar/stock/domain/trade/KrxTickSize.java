package me.singingsandhill.calendar.stock.domain.trade;

import me.singingsandhill.calendar.stock.domain.stock.StockProductType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * KRX 호가가격단위 (ADR stock/modes/0004).
 *
 * <ul>
 *   <li>주식(유가증권·코스닥, 2023-01-25 단일화): 2,000 미만 1 · 5,000 미만 5 · 20,000 미만 10 ·
 *       50,000 미만 50 · 200,000 미만 100 · 500,000 미만 500 · 이상 1,000</li>
 *   <li>ETF·ETN (2023-12-18 개정): 2,000 미만 1 · 이상 5</li>
 * </ul>
 *
 * 유형을 모르면(null) 주식 표를 쓴다. 밴드 경계값은 위·아래 두 단위의 공배수라, 아래 밴드
 * 단위로 올림해 경계를 넘어도 결과는 위 밴드에서도 유효한 가격이다.
 */
public final class KrxTickSize {

    private static final BigDecimal P2_000 = new BigDecimal("2000");
    private static final BigDecimal P5_000 = new BigDecimal("5000");
    private static final BigDecimal P20_000 = new BigDecimal("20000");
    private static final BigDecimal P50_000 = new BigDecimal("50000");
    private static final BigDecimal P200_000 = new BigDecimal("200000");
    private static final BigDecimal P500_000 = new BigDecimal("500000");

    private KrxTickSize() {
    }

    public static BigDecimal tickSize(BigDecimal price, StockProductType productType) {
        if (productType == StockProductType.ETF || productType == StockProductType.ETN) {
            return price.compareTo(P2_000) < 0 ? BigDecimal.ONE : BigDecimal.valueOf(5);
        }
        if (price.compareTo(P2_000) < 0) return BigDecimal.ONE;
        if (price.compareTo(P5_000) < 0) return BigDecimal.valueOf(5);
        if (price.compareTo(P20_000) < 0) return BigDecimal.TEN;
        if (price.compareTo(P50_000) < 0) return BigDecimal.valueOf(50);
        if (price.compareTo(P200_000) < 0) return BigDecimal.valueOf(100);
        if (price.compareTo(P500_000) < 0) return BigDecimal.valueOf(500);
        return BigDecimal.valueOf(1000);
    }

    /** 주문 가능한 가격으로 올림 (매수 체결가). */
    public static BigDecimal roundUp(BigDecimal price, StockProductType productType) {
        return round(price, productType, RoundingMode.CEILING);
    }

    /** 주문 가능한 가격으로 내림 (매도 체결가). */
    public static BigDecimal roundDown(BigDecimal price, StockProductType productType) {
        return round(price, productType, RoundingMode.FLOOR);
    }

    private static BigDecimal round(BigDecimal price, StockProductType productType, RoundingMode mode) {
        BigDecimal tick = tickSize(price, productType);
        return price.divide(tick, 0, mode).multiply(tick);
    }
}
