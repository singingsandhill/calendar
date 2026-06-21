package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.IndicatorResult;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P2-7: 모멘텀 가중 하향 — MA Trend ±15→±8, MA State ±10→±5.
 * 모멘텀이 과매도 평균회귀(RSI/Stoch level)를 ±40 임계 근처에서 상쇄하지 않도록.
 */
class SignalServiceWeightTest {

    private final SignalService svc = new SignalService(null, null, null, new TradingProperties());

    private BigDecimal bd(String s) {
        return s == null ? null : new BigDecimal(s);
    }

    private IndicatorResult ind(String price, String ma5, String ma20, String ma60) {
        return new IndicatorResult(bd(price), bd(ma5), bd(ma20), bd(ma60), null, null, null, null, null, 0);
    }

    private int maTrendScore(IndicatorResult i) throws Exception {
        Method m = SignalService.class.getDeclaredMethod("calculateMaTrendScore", IndicatorResult.class);
        m.setAccessible(true);
        return (int) m.invoke(svc, i);
    }

    private int maCrossScore(IndicatorResult i, BigDecimal[] prev) throws Exception {
        Method m = SignalService.class.getDeclaredMethod("calculateMaCrossScore", IndicatorResult.class, BigDecimal[].class);
        m.setAccessible(true);
        return (int) m.invoke(svc, new Object[]{i, prev});
    }

    @Test
    void maTrendScore_reducedToEight() throws Exception {
        assertThat(maTrendScore(ind("1100", null, null, "1000"))).isEqualTo(8);   // 가격 > MA60
        assertThat(maTrendScore(ind("900", null, null, "1000"))).isEqualTo(-8);   // 가격 < MA60
    }

    @Test
    void maCrossStateScore_reducedToFive() throws Exception {
        // 수렴 아님(gap 1% > 0.2%), 이벤트 아님(prev null) → 상태 점수
        assertThat(maCrossScore(ind("1000", "1010", "1000", null), null)).isEqualTo(5);   // ma5 > ma20
        assertThat(maCrossScore(ind("1000", "990", "1000", null), null)).isEqualTo(-5);   // ma5 < ma20
    }
}
