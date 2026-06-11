package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P2-6: RSI 추세는 최소 델타(기본 2점) 초과 변화에만 ±1, 그 외 0 (인접봉 잡음 동전던지기 제거).
 */
class IndicatorServiceRsiTrendTest {

    private IndicatorService service(double minDelta) {
        TradingProperties props = new TradingProperties();
        props.getIndicators().setMinRsiTrendDelta(minDelta);
        return new IndicatorService(null, props);
    }

    @Test
    void clearRise_returnsUptrend() {
        assertThat(service(2.0).rsiTrend(new BigDecimal("55"), new BigDecimal("40"))).isEqualTo(1);
    }

    @Test
    void clearFall_returnsDowntrend() {
        assertThat(service(2.0).rsiTrend(new BigDecimal("40"), new BigDecimal("55"))).isEqualTo(-1);
    }

    @Test
    void tinyChangeBelowMinDelta_returnsFlat() {
        assertThat(service(2.0).rsiTrend(new BigDecimal("51"), new BigDecimal("50"))).isZero();
    }

    @Test
    void exactlyAtMinDelta_returnsFlat() {
        // delta = 2.0, minDelta = 2.0 → '초과' 아님 → 0
        assertThat(service(2.0).rsiTrend(new BigDecimal("52"), new BigDecimal("50"))).isZero();
    }
}
