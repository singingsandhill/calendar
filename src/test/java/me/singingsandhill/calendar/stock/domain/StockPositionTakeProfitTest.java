package me.singingsandhill.calendar.stock.domain;

import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 다단계 익절 트리거 (PR-4 비순차화) 회귀 테스트.
 *
 * 의도: TP1 미실행이어도 TP2/TP3 조건 도달 시 독립적으로 trigger 가능해야 한다.
 */
class StockPositionTakeProfitTest {

    private static final BigDecimal TP1_PERCENT = new BigDecimal("1.5");
    private static final BigDecimal TP3_PERCENT = new BigDecimal("1.0");
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 1);

    private StockPosition newPosition(BigDecimal entry, BigDecimal dayHigh) {
        return StockPosition.open(
            "005930",
            TODAY,
            entry,
            100,
            entry.multiply(new BigDecimal("0.985")), // SL -1.5%
            dayHigh
        );
    }

    @Test
    void tp1_triggersAtPlus1_5Percent() {
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        assertThat(p.shouldTp1(new BigDecimal("101500"), TP1_PERCENT)).isTrue();
    }

    @Test
    void tp1_doesNotTriggerBelowThreshold() {
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        assertThat(p.shouldTp1(new BigDecimal("101400"), TP1_PERCENT)).isFalse();
    }

    @Test
    void tp2_triggersIndependentlyOfTp1() {
        // PR-4: TP1 미실행 상태에서도 TP2(당일고점) 도달 시 발동.
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        assertThat(p.shouldTp2(new BigDecimal("103000"))).isTrue();
        assertThat(p.shouldTp2(new BigDecimal("103200"))).isTrue();
    }

    @Test
    void tp3_triggersIndependentlyOfTp1AndTp2() {
        // PR-4: TP2 미실행 상태에서도 TP3(고점+1%) 도달 시 발동.
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        // dayHigh * 1.01 = 104030
        assertThat(p.shouldTp3(new BigDecimal("104030"), TP3_PERCENT)).isTrue();
        assertThat(p.shouldTp3(new BigDecimal("103500"), TP3_PERCENT)).isFalse();
    }

    @Test
    void stopLoss_triggersAtMinus1_5Percent() {
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        assertThat(p.shouldStopLoss(new BigDecimal("98500"))).isTrue();
        assertThat(p.shouldStopLoss(new BigDecimal("98600"))).isFalse();
    }

    @Test
    void unrealizedPnlPercent_isLinear() {
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        BigDecimal pnl = p.calculateUnrealizedPnlPercent(new BigDecimal("102000"));
        assertThat(pnl).isEqualByComparingTo("2.0000");
    }
}
