package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.candle.Candle;
import me.singingsandhill.calendar.trading.domain.candle.CandleRepository;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ATR 손절 모드용 집계봉 ATR (ADR trading/strategy/0011) — 1분봉 ATR 은 노이즈라 N분 집계봉으로 잰다.
 */
class IndicatorServiceAggregatedAtrTest {

    private final CandleRepository candleRepository = mock(CandleRepository.class);
    private final IndicatorService service = new IndicatorService(candleRepository, new TradingProperties());

    private static Candle c(int minutesAgo, String high, String low, String close) {
        BigDecimal h = new BigDecimal(high);
        BigDecimal l = new BigDecimal(low);
        BigDecimal cl = new BigDecimal(close);
        return new Candle(null, "KRW-ADA", LocalDateTime.of(2026, 10, 22, 10, 0).minusMinutes(minutesAgo),
            cl, h, l, cl, BigDecimal.ONE, BigDecimal.ONE, LocalDateTime.now());
    }

    @Test
    void aggregatesMinuteCandlesIntoBarsBeforeAtr() {
        // 최신순 1분봉 9개 → 3분봉 3개. 봉0: 고 105·저 98·종 102, 봉1: 고 101·저 95·종 97, 봉2: 고 100·저 94·종 96
        List<Candle> desc = new ArrayList<>(List.of(
            c(0, "105", "100", "102"), c(1, "104", "99", "101"), c(2, "103", "98", "100"),
            c(3, "101", "96", "97"), c(4, "100", "95", "98"), c(5, "99", "96", "97"),
            c(6, "100", "95", "96"), c(7, "99", "94", "95"), c(8, "98", "96", "97")));
        when(candleRepository.findByMarketOrderByDateTimeDesc("KRW-ADA", 9)).thenReturn(desc);

        // TR0 = max(7, |105-97|, |98-97|) = 8, TR1 = max(6, |101-96|, |95-96|) = 6 → ATR = 7 → 7 / 102 = 0.068627(6자리) × 100
        BigDecimal atrPercent = service.calculateAggregatedATRPercent("KRW-ADA", 3, 2);

        assertThat(atrPercent).isEqualByComparingTo("6.8627");
    }

    @Test
    void returnsNullWhenNotEnoughCandles() {
        when(candleRepository.findByMarketOrderByDateTimeDesc("KRW-ADA", 9)).thenReturn(List.of(
            c(0, "105", "100", "102")));

        assertThat(service.calculateAggregatedATRPercent("KRW-ADA", 3, 2)).isNull();
    }
}
