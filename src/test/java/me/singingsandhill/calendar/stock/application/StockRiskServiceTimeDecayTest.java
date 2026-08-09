package me.singingsandhill.calendar.stock.application;

import me.singingsandhill.calendar.stock.application.service.StockRiskService;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Time-decay minProfitThreshold 회귀 테스트.
 *
 *  시작점 {@code trading.trading-loop-start}(09:20): minProfitThreshold (기본 0.5%)
 *  종점   {@code exit.final-exit-time}(11:20):       0
 *  중간 시각: 선형 보간
 *
 * 종점이 코드 상수(09:10/15:15)가 아니라 설정에서 온다는 것이 이 테스트의 핵심이다 —
 * 상수 시절엔 봇의 운영 창(09:20~11:20)이 곡선의 약 36% 만 지나가 후반 구간이 죽은 코드였다.
 */
class StockRiskServiceTimeDecayTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private StockRiskService serviceAt(LocalTime time) {
        StockProperties props = new StockProperties();
        StockProperties.Risk risk = props.getRisk();
        risk.setTimeDecayEnabled(true);
        risk.setMinProfitThreshold(new BigDecimal("0.005"));     // 0.5%
        risk.setMinProfitThresholdLate(new BigDecimal("0.001")); // 0.1%

        Instant fixed = LocalDate.of(2026, 5, 1).atTime(time).atZone(KST).toInstant();
        Clock clock = Clock.fixed(fixed, KST);
        return new StockRiskService(null, null, null, props, clock, null);
    }

    private BigDecimal threshold(LocalTime time) throws Exception {
        StockRiskService service = serviceAt(time);
        Method m = StockRiskService.class.getDeclaredMethod("calculateTimeDecayThreshold");
        m.setAccessible(true);
        return (BigDecimal) m.invoke(service);
    }

    @Test
    void atTradingLoopStart_returnsEarlyThreshold() throws Exception {
        BigDecimal v = threshold(LocalTime.of(9, 20));
        assertThat(v).isEqualByComparingTo("0.005");
    }

    @Test
    void atFinalExitTime_returnsZero() throws Exception {
        BigDecimal v = threshold(LocalTime.of(11, 20));
        assertThat(v).isEqualByComparingTo("0");
    }

    @Test
    void midSession_returnsLinearlyInterpolated() throws Exception {
        // 09:20 -> 11:20 = 120 분. 10:20 = 50% progress
        // -> 0.005 - (0.005 - 0.001) * 0.5 = 0.003
        BigDecimal v = threshold(LocalTime.of(10, 20));
        assertThat(v.doubleValue()).isCloseTo(0.003, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    void beforeStart_returnsEarlyThreshold() throws Exception {
        BigDecimal v = threshold(LocalTime.of(9, 0));
        assertThat(v).isEqualByComparingTo("0.005");
    }

    /** 종점이 설정에서 온다 — 창을 좁히면 같은 시각의 임계가 달라져야 한다. */
    @Test
    void decayEndpointsComeFromConfigNotConstants() throws Exception {
        StockProperties props = new StockProperties();
        props.getRisk().setTimeDecayEnabled(true);
        props.getRisk().setMinProfitThreshold(new BigDecimal("0.005"));
        props.getRisk().setMinProfitThresholdLate(new BigDecimal("0.001"));
        props.getTrading().setTradingLoopStart("09:20");
        props.getExit().setFinalExitTime("10:20");

        Instant fixed = LocalDate.of(2026, 5, 1).atTime(LocalTime.of(9, 50)).atZone(KST).toInstant();
        StockRiskService service = new StockRiskService(null, null, null, props, Clock.fixed(fixed, KST), null);
        Method m = StockRiskService.class.getDeclaredMethod("calculateTimeDecayThreshold");
        m.setAccessible(true);

        // 창이 60분이면 09:50 은 50% 지점 -> 0.003 (기본 창 09:20~11:20 이라면 0.004)
        assertThat(((BigDecimal) m.invoke(service)).doubleValue())
            .isCloseTo(0.003, org.assertj.core.data.Offset.offset(0.0001));
    }
}
