package me.singingsandhill.calendar.stock.domain.position;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 주식 포지션 저장소 (Port)
 */
public interface StockPositionRepository {

    StockPosition save(StockPosition position);

    Optional<StockPosition> findById(Long id);

    Optional<StockPosition> findByStockCodeAndTradingDateAndStatusNot(
        String stockCode, LocalDate tradingDate, StockPositionStatus status);

    List<StockPosition> findByTradingDate(LocalDate tradingDate);

    List<StockPosition> findByTradingDateAndStatus(LocalDate tradingDate, StockPositionStatus status);

    List<StockPosition> findOpenPositions(LocalDate tradingDate);

    List<StockPosition> findClosedPositions(LocalDate tradingDate);

    int countOpenPositions(LocalDate tradingDate);

    void deleteById(Long id);

    void deleteByTradingDateBefore(LocalDate date);
}
