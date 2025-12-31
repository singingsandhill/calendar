package me.singingsandhill.calendar.stock.domain.trade;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 주식 거래 저장소 (Port)
 */
public interface StockTradeRepository {

    StockTrade save(StockTrade trade);

    Optional<StockTrade> findById(Long id);

    Optional<StockTrade> findByOrderId(String orderId);

    List<StockTrade> findByPositionId(Long positionId);

    List<StockTrade> findByStockCodeAndOrderedAtBetween(
        String stockCode, LocalDateTime from, LocalDateTime to);

    List<StockTrade> findByOrderedAtBetween(LocalDateTime from, LocalDateTime to);

    List<StockTrade> findTodayTrades();

    void deleteById(Long id);

    void deleteByOrderedAtBefore(LocalDateTime dateTime);
}
