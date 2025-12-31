package me.singingsandhill.calendar.stock.domain.stock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 감시 종목 저장소 (Port)
 */
public interface StockRepository {

    Stock save(Stock stock);

    List<Stock> saveAll(List<Stock> stocks);

    Optional<Stock> findById(Long id);

    Optional<Stock> findByStockCodeAndTradingDate(String stockCode, LocalDate tradingDate);

    List<Stock> findByTradingDate(LocalDate tradingDate);

    List<Stock> findByTradingDateAndState(LocalDate tradingDate, StockState state);

    List<Stock> findByTradingDateAndStateIn(LocalDate tradingDate, List<StockState> states);

    List<Stock> findByTradingDateOrderByGapPercentDesc(LocalDate tradingDate);

    List<Stock> findActiveStocks(LocalDate tradingDate);

    void deleteById(Long id);

    void deleteByTradingDateBefore(LocalDate date);

    int countByTradingDateAndState(LocalDate tradingDate, StockState state);
}
