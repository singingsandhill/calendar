package me.singingsandhill.calendar.stock.domain.candle;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주식 캔들 저장소 (Port)
 */
public interface StockCandleRepository {

    StockCandle save(StockCandle candle);

    List<StockCandle> saveAll(List<StockCandle> candles);

    Optional<StockCandle> findById(Long id);

    Optional<StockCandle> findByStockCodeAndCandleDateTimeAndInterval(
        String stockCode, LocalDateTime candleDateTime, CandleInterval interval);

    List<StockCandle> findByStockCodeAndIntervalOrderByDateTimeDesc(
        String stockCode, CandleInterval interval, int limit);

    Optional<StockCandle> findLatestByStockCodeAndInterval(String stockCode, CandleInterval interval);

    List<StockCandle> findByStockCodeAndIntervalAndDateTimeRange(
        String stockCode, CandleInterval interval, LocalDateTime from, LocalDateTime to);

    void deleteById(Long id);

    void deleteByDateTimeBefore(LocalDateTime dateTime);
}
