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
 *  09:10 시작점: minProfitThreshold (기본 0.5%)
 *  15:15 종점:   0 (또는 minProfitThresholdLate)
 *  중간 시각:    선형 보간
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
        return new StockRiskService(null, null, null, props, clock);
    }

    private BigDecimal threshold(LocalTime time) throws Exception {
        StockRiskService service = serviceAt(time);
        Method m = StockRiskService.class.getDeclaredMethod("calculateTimeDecayThreshold");
        m.setAccessible(true);
        return (BigDecimal) m.invoke(service);
    }

    @Test
    void atStart_returnsEarlyThreshold() throws Exception {
        BigDecimal v = threshold(LocalTime.of(9, 10));
        assertThat(v).isEqualByComparingTo("0.005");
    }

    @Test
    void afterEnd_returnsZero() throws Exception {
        BigDecimal v = threshold(LocalTime.of(15, 15));
        assertThat(v).isEqualByComparingTo("0");
    }

    @Test
    void midSession_returnsLinearlyInterpolated() throws Exception {
        // 09:10 -> 15:15 = 365 분
        // 12:12.5 ≈ 50% progress -> 0.005 - (0.005 - 0.001) * 0.5 = 0.003
        BigDecimal v = threshold(LocalTime.of(12, 13));
        assertThat(v.doubleValue()).isCloseTo(0.003, org.assertj.core.data.Offset.offset(0.0002));
    }

    @Test
    void beforeStart_returnsEarlyThreshold() throws Exception {
        BigDecimal v = threshold(LocalTime.of(9, 0));
        assertThat(v).isEqualByComparingTo("0.005");
    }
}
