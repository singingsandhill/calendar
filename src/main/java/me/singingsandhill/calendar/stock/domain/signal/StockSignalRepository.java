package me.singingsandhill.calendar.stock.domain.signal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주식 신호 저장소 (Port)
 */
public interface StockSignalRepository {

    StockSignal save(StockSignal signal);

    Optional<StockSignal> findById(Long id);

    List<StockSignal> findByStockCodeAndSignalTimeBetween(
        String stockCode, LocalDateTime from, LocalDateTime to);

    List<StockSignal> findBySignalTimeBetween(LocalDateTime from, LocalDateTime to);

    List<StockSignal> findByStockCodeAndSignalType(String stockCode, StockSignalType signalType);

    List<StockSignal> findTodaySignals();

    void deleteById(Long id);

    void deleteBySignalTimeBefore(LocalDateTime dateTime);
}
