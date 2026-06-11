package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.application.dto.IndicatorResult;
import me.singingsandhill.calendar.trading.domain.candle.Candle;
import me.singingsandhill.calendar.trading.domain.candle.CandleRepository;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P2-5: 스코어링이 사용하는 stochK 필드를 raw fast %K → slow %K(fast %K 의 stochSlow SMA)로 전환.
 * (dead config stochSlow 활용 + 1분봉 잡음 평활.)
 */
class IndicatorServiceSlowStochTest {

    private static final String MARKET = "KRW-ADA";
    private static final LocalDateTime T = LocalDateTime.of(2026, 5, 30, 12, 0);

    private final CandleRepository repo = mock(CandleRepository.class);
    private final IndicatorService svc = new IndicatorService(repo, new TradingProperties());

    private Candle candle(double close) {
        BigDecimal c = BigDecimal.valueOf(close);
        // high=120, low=80 고정 범위. open=trade=close.
        return new Candle(null, MARKET, T, c, BigDecimal.valueOf(120), BigDecimal.valueOf(80),
                c, BigDecimal.ONE, BigDecimal.ONE, T);
    }

    @Test
    void calculate_stochK_usesSlowStochastic() {
        // requiredCandles = max(maLong 60, rsiPeriod 14) + 20 = 80
        List<Candle> candles = new ArrayList<>();
        candles.add(candle(100)); // idx0 (최신): fast %K = (100-80)/40*100 = 50
        candles.add(candle(110)); // idx1: 75
        candles.add(candle(120)); // idx2: 100
        for (int i = 3; i < 80; i++) candles.add(candle(100));
        when(repo.findByMarketOrderByDateTimeDesc(MARKET, 80)).thenReturn(candles);

        IndicatorResult r = svc.calculate(MARKET);

        // slow %K = avg(50, 75, 100) = 75 (fast %K 50 이 아니라)
        assertThat(r.stochK().doubleValue()).isCloseTo(75.0, offset(0.5));
    }
}
