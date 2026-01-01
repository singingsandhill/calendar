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

    public boolean isGoldenCross() {
        if (ma5 == null || ma20 == null) return false;
        return ma5.compareTo(ma20) > 0;
    }

    public boolean isDeathCross() {
        if (ma5 == null || ma20 == null) return false;
        return ma5.compareTo(ma20) < 0;
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
