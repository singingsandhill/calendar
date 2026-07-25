package me.singingsandhill.calendar.stock.domain;

import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
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
    private static final BigDecimal TRAILING_PERCENT = new BigDecimal("3.8");
    private static final BigDecimal FEE = new BigDecimal("0.00015");
    private static final BigDecimal TAX = new BigDecimal("0.0015");
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
        // PR-4: TP2 미실행 상태에서도 TP3 도달 시 발동.
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        // 진입가 고정 앵커: 100000 * 1.01 = 101000
        assertThat(p.shouldTp3(new BigDecimal("101000"), TP3_PERCENT)).isTrue();
        assertThat(p.shouldTp3(new BigDecimal("100900"), TP3_PERCENT)).isFalse();
    }

    @Test
    void tp3_anchorIsEntryPriceNotMovingDayHigh() {
        // 2026-07-24 리뷰 §4 / P1-3: 당일고가는 매 틱 갱신되므로 앵커로 쓰면 5초 내 +N% 점프를
        // 요구해 수학적으로 도달 불가였다. 진입가 고정 앵커라야 도달 가능해야 한다.
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        p.updateDayHighPrice(new BigDecimal("105000")); // 고가가 올라가도 TP3 목표는 불변

        assertThat(p.shouldTp3(new BigDecimal("101000"), TP3_PERCENT)).isTrue();
    }

    // ===== 트레일링 스탑 (2026-07-24 리뷰 §4 / P1-2) =====

    @Test
    void trailing_activatesAfterTp2NotOnlyTp1() {
        // TP2(전고점 회복)가 TP1 보다 먼저 발동하는 것이 통상 경로 — tp1Executed 만 조건이면
        // 트레일링이 영원히 활성화되지 않는다.
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        p.executePartialExit(60, new BigDecimal("103000"), StockCloseReason.TP2, FEE, TAX);

        p.updateTrailingStop(new BigDecimal("103000"), TRAILING_PERCENT, null);

        assertThat(p.isTrailingActive()).isTrue();
    }

    @Test
    void trailing_setsStopPriceImmediatelyOnActivation() {
        // 활성화 직후 하락만 하면 신고가 갱신 블록에 못 들어가 스탑가가 null 인 채 미발동했다.
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        p.executePartialExit(60, new BigDecimal("103000"), StockCloseReason.TP2, FEE, TAX);

        p.updateTrailingStop(new BigDecimal("103000"), TRAILING_PERCENT, null);

        // 103000 * (1 - 3.8%) = 99086
        assertThat(p.getTrailingStopPrice()).isEqualByComparingTo("99086");
        assertThat(p.shouldTrailingStop(new BigDecimal("99000"))).isTrue();
    }

    @Test
    void trailing_activationRespectsBreakEvenFloor() {
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        p.executePartialExit(60, new BigDecimal("103000"), StockCloseReason.TP2, FEE, TAX);
        BigDecimal breakEven = new BigDecimal("100260");

        p.updateTrailingStop(new BigDecimal("103000"), TRAILING_PERCENT, breakEven);

        // 99086 < breakEven → breakEven 으로 올려 잡는다
        assertThat(p.getTrailingStopPrice()).isEqualByComparingTo("100260");
    }

    // ===== TP1 수량: 설정 비율 + 잔여수량 캡 (2026-07-24 리뷰 §3-⑤) =====

    @Test
    void tp1Quantity_usesConfiguredRatio() {
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        assertThat(p.calculateTp1Quantity(new BigDecimal("0.5"))).isEqualTo(50);
        assertThat(p.calculateTp1Quantity(new BigDecimal("0.4"))).isEqualTo(40);
    }

    @Test
    void tp2Quantity_usesConfiguredRatio() {
        // exit.tp2-ratio 가 하드코딩 0.6 이던 팬텀 설정을 실제 배선 — 설정값이 수량을 결정해야 한다
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        assertThat(p.calculateTp2Quantity(new BigDecimal("0.6"))).isEqualTo(60);
        assertThat(p.calculateTp2Quantity(new BigDecimal("0.4"))).isEqualTo(40);
    }

    @Test
    void tp1Quantity_isCappedByRemainingAfterTp2() {
        // TP2(전고점 회복)가 TP1(+N%)보다 먼저 발동하는 것이 통상 경로 — 잔여 40 에서
        // entry×0.5=50 을 시도하면 초과 매도가 된다. 잔여수량으로 캡되어야 한다.
        StockPosition p = newPosition(new BigDecimal("100000"), new BigDecimal("103000"));
        p.executePartialExit(60, new BigDecimal("103000"), StockCloseReason.TP2,
            new BigDecimal("0.00015"), new BigDecimal("0.0015"));

        assertThat(p.getRemainingQuantity()).isEqualTo(40);
        assertThat(p.calculateTp1Quantity(new BigDecimal("0.5"))).isEqualTo(40);
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
