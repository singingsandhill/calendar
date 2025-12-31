package me.singingsandhill.calendar.trading.domain.candle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CandleRepository {

    Candle save(Candle candle);

    List<Candle> saveAll(List<Candle> candles);

    Optional<Candle> findById(Long id);

    List<Candle> findByMarketOrderByDateTimeDesc(String market, int limit);

    Optional<Candle> findLatestByMarket(String market);

    Optional<Candle> findByMarketAndDateTime(String market, LocalDateTime dateTime);

    Optional<Candle> findByMarketAndCandleDateTime(String market, LocalDateTime candleDateTime);

    List<Candle> findByMarketAndDateTimeRange(String market, LocalDateTime from, LocalDateTime to);

    void deleteOlderThan(LocalDateTime dateTime);

    int deleteByDateTimeBefore(LocalDateTime dateTime);

    long countByMarket(String market);
}
