package me.singingsandhill.calendar.trading.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 레짐·리밸런싱 설정 기본값 (ADR trading/risk/0007) — 결론이 안 난 값은 기본 = 현재 동작(필터 OFF),
 * 데이터가 결론을 낸 리밸런싱은 기본 OFF. Java 기본값이 yaml 운영값과 어긋나면 키 누락 시 조용히 회귀한다(P1-9).
 */
class TradingRegimePropertiesTest {

    @Test
    void regimeFilterDefaultsToOffWithFailSafeUnknownPolicy() {
        TradingProperties.Regime regime = new TradingProperties().getRegime();

        assertThat(regime.getFilterMode()).isEqualTo(TradingProperties.Regime.FilterMode.OFF);
        assertThat(regime.getUnknownPolicy()).isEqualTo(TradingProperties.Regime.UnknownPolicy.BLOCK);
        assertThat(regime.getBearSizeMultiplier()).isEqualTo(0.5);
        assertThat(regime.getMaDays()).isEqualTo(20);
        assertThat(regime.getHysteresisPct()).isEqualTo(0.02);
        assertThat(regime.getMinDays()).isEqualTo(3);
    }

    @Test
    void rebalancingDefaultsToOffWithDailyRegimeAndCatastrophicStop() {
        TradingProperties.Rebalancing rebalancing = new TradingProperties().getRebalancing();

        assertThat(rebalancing.isEnabled()).isFalse();
        assertThat(rebalancing.getRegimeSource()).isEqualTo(TradingProperties.Rebalancing.RegimeSource.DAILY);
        assertThat(rebalancing.getCatastrophicStopPct()).isEqualTo(-0.15);
    }
}
