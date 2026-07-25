package me.singingsandhill.calendar.stock.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 거래 비용 모델 회귀 테스트 (2026-07-24 리뷰 §4 / P2-1).
 *
 * 2026-01-01 이후 양도분 세율: 코스피 = 증권거래세 0.05% + 농특세 0.15%,
 * 코스닥·K-OTC = 0.20%(농특세 없음) → 두 시장 모두 매도측 총 0.20%.
 * 코드 상수는 0.23%(구 세율)로 과대 계상돼 있었다.
 */
class StockCostModelTest {

    private final StockProperties.Risk risk = new StockProperties().getRisk();

    @Test
    void sellTaxRate_matches2026Rate() {
        assertThat(risk.getSellTaxRate()).isEqualByComparingTo("0.0020");
    }

    @Test
    void roundTripFeeRate_isCommissionTwicePlusSellTax() {
        // 0.00015 × 2 + 0.0020 = 0.0023
        assertThat(risk.getRoundTripFeeRate()).isEqualByComparingTo("0.0023");
    }

    @Test
    void effectiveExitCostRate_includesSlippageBuffer() {
        // TP 게이트는 왕복 수수료·세금만이 아니라 시장가 슬리피지도 넘어야 순익이다.
        // 0.0023 + 0.002 = 0.0043
        assertThat(risk.getEffectiveExitCostRate()).isEqualByComparingTo("0.0043");
    }

    @Test
    void effectiveExitCostRate_ignoresSlippageWhenDisabled() {
        StockProperties.Risk r = new StockProperties().getRisk();
        r.setSlippageBuffer(BigDecimal.ZERO);
        assertThat(r.getEffectiveExitCostRate()).isEqualByComparingTo("0.0023");
    }
}
