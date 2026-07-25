package me.singingsandhill.calendar.stock.domain;

import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 풀백저가 기반 동적 손절 (2026-07-24 리뷰 §4 / P1-4).
 *
 * 기존: 진입가 -5% 고정 → 현실적 익절(+0.8~5% 의 60% 청산) 대비 R:R 역전, 손익분기 승률 ~83%.
 * 변경: 진입 근거(풀백저가 반등)가 깨지는 지점에서 손절하되, 진입가 대비 최대 손실률로 캡.
 *   SL = max(풀백저가 × (1 - buffer%), 진입가 × (1 - maxLoss%))
 */
class StockPositionStopLossTest {

    private static final BigDecimal BUFFER = new BigDecimal("1.0");   // 풀백저가 -1%
    private static final BigDecimal MAX_LOSS = new BigDecimal("2.0"); // 진입가 -2% 캡

    @Test
    void usesPullbackLowAnchorWhenTighterThanCap() {
        // 진입가 = 풀백저가 × 1.002 (bounce 0.2%) 인 통상 경로
        BigDecimal pullbackLow = new BigDecimal("99800");
        BigDecimal entry = new BigDecimal("100000");

        BigDecimal sl = StockPosition.resolveStopLossPrice(entry, pullbackLow, BUFFER, MAX_LOSS);

        // 99800 × 0.99 = 98802 (진입가 대비 -1.198%) — 캡(98000)보다 높으므로 채택
        assertThat(sl).isEqualByComparingTo("98802");
    }

    @Test
    void capsLossWhenFillDriftedFarAbovePullbackLow() {
        // 체결가가 풀백저가보다 크게 위에서 잡히면 풀백 앵커는 -4% 로 벌어진다 → 캡이 걸려야 한다
        BigDecimal pullbackLow = new BigDecimal("96000");
        BigDecimal entry = new BigDecimal("100000");

        BigDecimal sl = StockPosition.resolveStopLossPrice(entry, pullbackLow, BUFFER, MAX_LOSS);

        // 96000 × 0.99 = 95040 (-4.96%) vs 캡 98000 (-2%) → 더 타이트한 98000
        assertThat(sl).isEqualByComparingTo("98000");
    }

    @Test
    void fallsBackToCapWhenPullbackLowMissing() {
        BigDecimal sl = StockPosition.resolveStopLossPrice(
            new BigDecimal("100000"), null, BUFFER, MAX_LOSS);

        assertThat(sl).isEqualByComparingTo("98000");
    }

    @Test
    void neverPlacesStopAtOrAboveEntryPrice() {
        // 풀백저가가 진입가보다 높은 비정상 상태(데이터 이상)에서도 손절이 진입가 위로 가면 안 된다
        BigDecimal sl = StockPosition.resolveStopLossPrice(
            new BigDecimal("100000"), new BigDecimal("105000"), BUFFER, MAX_LOSS);

        assertThat(sl).isEqualByComparingTo("98000");
    }
}
