package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.candle.Candle;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

/**
 * P2-4: RSI 를 Wilder 평활(표준)로 전환. 기존 단순평균(Cutler) RSI 와 값이 다르다.
 * 캔들은 최신순(DESC) — index 0 이 최신.
 */
class IndicatorServiceWilderRsiTest {

    private final IndicatorService indicatorService = new IndicatorService(null, null);

    private static final LocalDateTime T = LocalDateTime.of(2026, 5, 30, 12, 0);

    private Candle candle(double price) {
        BigDecimal p = BigDecimal.valueOf(price);
        return new Candle(null, "KRW-ADA", T, p, p, p, p, BigDecimal.ONE, BigDecimal.ONE, T);
    }

    @Test
    void allGains_returns100() {
        // DESC: 최신 115 ... 오래된 101 (시간순 단조 증가) → 손실 0 → RSI 100
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 15; i++) candles.add(candle(115 - i));
        assertThat(indicatorService.calculateRSI(candles, 14).doubleValue()).isCloseTo(100.0, offset(0.001));
    }

    @Test
    void allLosses_returns0() {
        // DESC: 최신 100 ... 오래된 114 (시간순 단조 감소) → 이익 0 → RSI 0
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 15; i++) candles.add(candle(100 + i));
        assertThat(indicatorService.calculateRSI(candles, 14).doubleValue()).isCloseTo(0.0, offset(0.001));
    }

    @Test
    void mixedSeries_usesWilderSmoothing() {
        // DESC [12,10,11,9], period 2. 시간순 변화: +2(seed gain), -1(seed loss), +2(smooth).
        // seed avgGain=1, avgLoss=0.5 → smooth avgGain=1.5, avgLoss=0.25 → rs=6 → RSI=85.714
        // (단순평균 RSI 였다면 66.67 → 본 테스트는 현재 코드에서 RED)
        List<Candle> candles = List.of(candle(12), candle(10), candle(11), candle(9));
        assertThat(indicatorService.calculateRSI(candles, 2).doubleValue()).isCloseTo(85.714, offset(0.05));
    }
}
