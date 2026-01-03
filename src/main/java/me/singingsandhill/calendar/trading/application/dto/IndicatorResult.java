package me.singingsandhill.calendar.trading.application.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public record IndicatorResult(
    BigDecimal currentPrice,
    BigDecimal ma5,
    BigDecimal ma20,
    BigDecimal ma60,
    BigDecimal rsi,
    BigDecimal stochK,
    BigDecimal stochD,
    BigDecimal volumeMa,
    BigDecimal currentVolume,
    int rsiTrend
) {
    public Map<String, BigDecimal> toMap() {
        Map<String, BigDecimal> map = new HashMap<>();
        map.put("currentPrice", currentPrice);
        map.put("ma5", ma5);
        map.put("ma20", ma20);
        map.put("ma60", ma60);
        map.put("rsi", rsi);
        map.put("stochK", stochK);
        map.put("stochD", stochD);
        map.put("volumeMa", volumeMa);
        map.put("currentVolume", currentVolume);
        return map;
    }

    /**
     * MA5 > MA20 상태 확인 (현재 상태만)
     * @deprecated 실제 크로스 이벤트가 아님. isMa5AboveMa20() 또는 isGoldenCross(prevMa5, prevMa20) 사용
     */
    @Deprecated
    public boolean isGoldenCross() {
        return isMa5AboveMa20();
    }

    /**
     * MA5 < MA20 상태 확인 (현재 상태만)
     * @deprecated 실제 크로스 이벤트가 아님. isMa5BelowMa20() 또는 isDeathCross(prevMa5, prevMa20) 사용
     */
    @Deprecated
    public boolean isDeathCross() {
        return isMa5BelowMa20();
    }

    /**
     * MA5가 MA20 위에 있는지 확인 (현재 상태)
     */
    public boolean isMa5AboveMa20() {
        if (ma5 == null || ma20 == null) return false;
        return ma5.compareTo(ma20) > 0;
    }

    /**
     * MA5가 MA20 아래에 있는지 확인 (현재 상태)
     */
    public boolean isMa5BelowMa20() {
        if (ma5 == null || ma20 == null) return false;
        return ma5.compareTo(ma20) < 0;
    }

    /**
     * 실제 골든크로스 이벤트 감지
     * 이전에 MA5 <= MA20이었다가 현재 MA5 > MA20으로 전환된 경우
     */
    public boolean isGoldenCross(BigDecimal prevMa5, BigDecimal prevMa20) {
        if (ma5 == null || ma20 == null || prevMa5 == null || prevMa20 == null) {
            return false;
        }
        // 이전: MA5 <= MA20, 현재: MA5 > MA20
        return prevMa5.compareTo(prevMa20) <= 0 && ma5.compareTo(ma20) > 0;
    }

    /**
     * 실제 데드크로스 이벤트 감지
     * 이전에 MA5 >= MA20이었다가 현재 MA5 < MA20으로 전환된 경우
     */
    public boolean isDeathCross(BigDecimal prevMa5, BigDecimal prevMa20) {
        if (ma5 == null || ma20 == null || prevMa5 == null || prevMa20 == null) {
            return false;
        }
        // 이전: MA5 >= MA20, 현재: MA5 < MA20
        return prevMa5.compareTo(prevMa20) >= 0 && ma5.compareTo(ma20) < 0;
    }

    public boolean isPriceAboveMa60() {
        if (currentPrice == null || ma60 == null) return false;
        return currentPrice.compareTo(ma60) > 0;
    }

    public boolean isPriceBelowMa60() {
        if (currentPrice == null || ma60 == null) return false;
        return currentPrice.compareTo(ma60) < 0;
    }

    public boolean isRsiUptrend() {
        return rsiTrend > 0;
    }

    public boolean isRsiDowntrend() {
        return rsiTrend < 0;
    }
}
