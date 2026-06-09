package me.singingsandhill.calendar.trading.application.service;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P2-1: 다이버전스 피벗 강화. 좌우 k봉(k=3) 비교로 1분봉 단일봉 잡음 피벗 제거.
 * (기존 3봉 피벗 i-1,i,i+1 은 노이즈 스파이크도 피벗으로 잡음.)
 */
class DivergenceServicePivotTest {

    private final DivergenceService svc = new DivergenceService(null, null, null);

    @SuppressWarnings("unchecked")
    private List<Integer> minima(List<BigDecimal> values) throws Exception {
        Method m = DivergenceService.class.getDeclaredMethod("findLocalMinima", List.class, int.class);
        m.setAccessible(true);
        return (List<Integer>) m.invoke(svc, values, 20);
    }

    private List<BigDecimal> vals(double... xs) {
        List<BigDecimal> l = new ArrayList<>();
        for (double x : xs) l.add(BigDecimal.valueOf(x));
        return l;
    }

    @Test
    void singleBarDip_notDetectedAsPivot() throws Exception {
        // idx5=4.5 는 즉시 이웃(idx4=5, idx6=6)보다 낮아 3봉 피벗이지만,
        // idx3=4 < 4.5 라 k=3 피벗은 아님 → 잡음으로 거르기.
        List<BigDecimal> v = vals(1, 2, 3, 4, 5, 4.5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
        assertThat(minima(v)).doesNotContain(5);
    }

    @Test
    void clearKBarValley_detected() throws Exception {
        // idx5=0 은 좌우 3봉(2,4,6 / 2,4,6) 모두보다 낮은 명확한 밸리 → 검출.
        List<BigDecimal> v = vals(10, 8, 6, 4, 2, 0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28);
        assertThat(minima(v)).contains(5);
    }
}
