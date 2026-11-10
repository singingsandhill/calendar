package me.singingsandhill.calendar.trading.domain.trade;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 빗썸 원화(KRW) 마켓 호가단위 (ADR trading/strategy/0011) — 2026-10-22 공개 호가창 실측 + 고객지원 원화 마켓 거래 정책.
 *
 * <p>1 미만 0.0001 · 10 미만 0.001 · 100 미만 0.01 · 5,000 미만 1 · 10,000 미만 5 · 50,000 미만 10 ·
 * 100,000 미만 50 · 500,000 미만 100 · 1,000,000 미만 500 · 이상 1,000. 경계 가격은 위 구간에 속한다.
 *
 * 밴드 경계값은 위·아래 두 단위의 공배수라, 아래 밴드 단위로 올림해 경계를 넘어도 위 밴드에서 유효한 가격이다.
 */
public final class BithumbTickSize {

    private static final BigDecimal P1 = BigDecimal.ONE;
    private static final BigDecimal P10 = BigDecimal.TEN;
    private static final BigDecimal P100 = new BigDecimal("100");
    private static final BigDecimal P5_000 = new BigDecimal("5000");
    private static final BigDecimal P10_000 = new BigDecimal("10000");
    private static final BigDecimal P50_000 = new BigDecimal("50000");
    private static final BigDecimal P100_000 = new BigDecimal("100000");
    private static final BigDecimal P500_000 = new BigDecimal("500000");
    private static final BigDecimal P1_000_000 = new BigDecimal("1000000");

    private BithumbTickSize() {
    }

    public static BigDecimal tickSize(BigDecimal price) {
        if (price.compareTo(P1) < 0) return new BigDecimal("0.0001");
        if (price.compareTo(P10) < 0) return new BigDecimal("0.001");
        if (price.compareTo(P100) < 0) return new BigDecimal("0.01");
        if (price.compareTo(P5_000) < 0) return BigDecimal.ONE;
        if (price.compareTo(P10_000) < 0) return BigDecimal.valueOf(5);
        if (price.compareTo(P50_000) < 0) return BigDecimal.TEN;
        if (price.compareTo(P100_000) < 0) return BigDecimal.valueOf(50);
        if (price.compareTo(P500_000) < 0) return BigDecimal.valueOf(100);
        if (price.compareTo(P1_000_000) < 0) return BigDecimal.valueOf(500);
        return BigDecimal.valueOf(1000);
    }

    /** 호가 격자로 올림 (손익분기가). */
    public static BigDecimal roundUp(BigDecimal price) {
        return round(price, RoundingMode.CEILING);
    }

    /** 호가 격자로 내림 (손절가·트레일링 스탑). */
    public static BigDecimal roundDown(BigDecimal price) {
        return round(price, RoundingMode.FLOOR);
    }

    private static BigDecimal round(BigDecimal price, RoundingMode mode) {
        BigDecimal tick = tickSize(price);
        return price.divide(tick, 0, mode).multiply(tick);
    }
}
