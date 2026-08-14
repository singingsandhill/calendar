package me.singingsandhill.calendar.trading.application.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    int rsiTrend,
    // 변동성. Signal 로 함께 영속화해 사후 분석에서 쓴다 — 예전에는 TradingBotService 가
    // 계산해 쓰고 버려서 포지션 사이징 결정을 재구성할 수 없었다 (ADR trading/observability/0002).
    // 기존 10개 인자 뒤에 append 한다 — 위치 인자가 밀리면 조용히 값이 뒤바뀐다.
    BigDecimal atr
) {
    /**
     * 가격 대비 변동성(%). 저장·분석용 파생값이며 상태를 따로 들지 않는다.
     *
     * <p>주문 사이징은 계속 {@code IndicatorService.calculateATRPercent(market)} 를 쓴다 —
     * 그쪽은 자체적으로 캔들을 다시 읽고 {@code excludeFormingCandle} 을 따르지 않으므로,
     * 그 플래그를 켜면 두 값이 갈라질 수 있다. 그래서 실제 적용된 주문 비중은 별도로
     * {@code trading_trades.order_ratio} 에 기록한다.
     */
    public BigDecimal atrPercent() {
        if (atr == null || currentPrice == null || currentPrice.signum() == 0) {
            return null;
        }
        return atr.divide(currentPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

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
        map.put("atr", atr);
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
