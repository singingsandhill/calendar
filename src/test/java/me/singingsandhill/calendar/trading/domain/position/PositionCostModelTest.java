package me.singingsandhill.calendar.trading.domain.position;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * P1-8: 비용모델 잠금. calculateUnrealizedPnlPctWithFee 는 *왕복 수수료* 를 차감한 net PnL 이다.
 * 따라서 매도 게이트 minProfitThreshold 는 수수료를 다시 더하면 안 되고 *순수 마진* 만이어야 한다.
 */
class PositionCostModelTest {

    private static final String MARKET = "KRW-ADA";
    private static final BigDecimal FEE_RATE = new BigDecimal("0.0025"); // 0.25%

    @Test
    void atFlatPrice_netPnlEqualsNegativeRoundTripFee() {
        // 진입 1000 × 10 = 10,000, 진입 수수료 25(0.25%). 현재가 = 진입가(평탄).
        Position pos = Position.open(MARKET, new BigDecimal("1000"), new BigDecimal("10"),
                new BigDecimal("970"), new BigDecimal("1150"), new BigDecimal("25"));

        BigDecimal pnlPct = pos.calculateUnrealizedPnlPctWithFee(new BigDecimal("1000"), FEE_RATE);

        // 진입 수수료 25 + 예상 청산 수수료 25 = 50 → -0.5% (왕복 수수료). 가격 마진은 0.
        assertThat(pnlPct.doubleValue()).isCloseTo(-0.5, offset(0.001));
    }

    @Test
    void breakEvenAfterFee_requiresGrossMoveAboveRoundTripFee() {
        Position pos = Position.open(MARKET, new BigDecimal("1000"), new BigDecimal("10"),
                new BigDecimal("970"), new BigDecimal("1150"), new BigDecimal("25"));

        // 가격 +1%(1010): 총가치 10,100, 청산 수수료 25.25, 총수수료 50.25 → net (10100-10000-50.25)/10000 = +0.4975%
        BigDecimal pnlPct = pos.calculateUnrealizedPnlPctWithFee(new BigDecimal("1010"), FEE_RATE);
        assertThat(pnlPct.doubleValue()).isCloseTo(0.4975, offset(0.01));
    }
}
