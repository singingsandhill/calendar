package me.singingsandhill.calendar.trading.domain.trade;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 빗썸 원화 마켓 호가단위 (ADR trading/strategy/0011) — 2026-10-22 공개 호가창 실측 + 고객지원 원화 마켓 거래 정책.
 *
 * ADA ≈ 290원에서 1틱(1원) = 0.34% — 트레일링 0.8% 는 2.4틱, 손절 1.5% 는 4.4틱으로 노이즈 수준이었다.
 */
class BithumbTickSizeTest {

    private static BigDecimal tick(String price) {
        return BithumbTickSize.tickSize(new BigDecimal(price));
    }

    @Test
    void krwMarketBands() {
        assertThat(tick("0.0068")).isEqualByComparingTo("0.0001");
        assertThat(tick("1.026")).isEqualByComparingTo("0.001");
        assertThat(tick("72.55")).isEqualByComparingTo("0.01");
        assertThat(tick("290")).isEqualByComparingTo("1");
        assertThat(tick("999")).isEqualByComparingTo("1");
        assertThat(tick("1774")).isEqualByComparingTo("1");
        assertThat(tick("5140")).isEqualByComparingTo("5");
        assertThat(tick("12360")).isEqualByComparingTo("10");
        assertThat(tick("50000")).isEqualByComparingTo("50");
        assertThat(tick("139600")).isEqualByComparingTo("100");
        assertThat(tick("980000")).isEqualByComparingTo("500");
        assertThat(tick("109390000")).isEqualByComparingTo("1000");
    }

    @Test
    void bandBoundariesBelongToTheUpperBand() {
        assertThat(tick("1")).isEqualByComparingTo("0.001");
        assertThat(tick("10")).isEqualByComparingTo("0.01");
        assertThat(tick("100")).isEqualByComparingTo("1");
        assertThat(tick("5000")).isEqualByComparingTo("5");
        assertThat(tick("10000")).isEqualByComparingTo("10");
        assertThat(tick("100000")).isEqualByComparingTo("100");
        assertThat(tick("500000")).isEqualByComparingTo("500");
        assertThat(tick("1000000")).isEqualByComparingTo("1000");
    }

    @Test
    void roundingFollowsTheTickGrid() {
        assertThat(BithumbTickSize.roundDown(new BigDecimal("289.9"))).isEqualByComparingTo("289");
        assertThat(BithumbTickSize.roundUp(new BigDecimal("289.1"))).isEqualByComparingTo("290");
        assertThat(BithumbTickSize.roundDown(new BigDecimal("72.556"))).isEqualByComparingTo("72.55");
    }
}
