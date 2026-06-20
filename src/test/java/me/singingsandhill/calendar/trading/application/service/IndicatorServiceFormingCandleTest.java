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
 * P2-2: excludeFormingCandle=true 면 지표 계산에서 형성 중(index 0) 봉을 제외한다.
 * currentPrice 는 라이브(index 0 tradePrice) 유지.
 */
class IndicatorServiceFormingCandleTest {

    private static final String MARKET = "KRW-ADA";
    private static final LocalDateTime T = LocalDateTime.of(2026, 5, 30, 12, 0);

    private final CandleRepository repo = mock(CandleRepository.class);

    private IndicatorService service(boolean exclude) {
        TradingProperties props = new TradingProperties();
        props.getIndicators().setExcludeFormingCandle(exclude);
        return new IndicatorService(repo, props);
    }

    private Candle candle(double close) {
        BigDecimal c = BigDecimal.valueOf(close);
        return new Candle(null, MARKET, T, c, c, c, c, BigDecimal.ONE, BigDecimal.ONE, T);
    }

    @Test
    void excludeFormingCandle_maComputedWithoutFormingBar() {
        List<Candle> candles = new ArrayList<>();
        candles.add(candle(9999)); // index 0: 형성 중 스파이크
        for (int i = 1; i < 80; i++) candles.add(candle(100));
        when(repo.findByMarketOrderByDateTimeDesc(MARKET, 80)).thenReturn(candles);

        IndicatorResult r = service(true).calculate(MARKET);

        // MA5 는 형성봉(9999) 제외 → 100 부근. currentPrice 는 라이브 9999 유지.
        assertThat(r.ma5().doubleValue()).isCloseTo(100.0, offset(0.5));
        assertThat(r.currentPrice()).isEqualByComparingTo("9999");
    }
}
