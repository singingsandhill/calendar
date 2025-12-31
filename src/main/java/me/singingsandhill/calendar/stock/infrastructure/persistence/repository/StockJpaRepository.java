package me.singingsandhill.calendar.stock.infrastructure.persistence.repository;

import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockJpaRepository extends JpaRepository<StockJpaEntity, Long> {

    Optional<StockJpaEntity> findByStockCodeAndTradingDate(String stockCode, LocalDate tradingDate);

    List<StockJpaEntity> findByTradingDate(LocalDate tradingDate);

    List<StockJpaEntity> findByTradingDateAndState(LocalDate tradingDate, String state);

    List<StockJpaEntity> findByTradingDateAndStateIn(LocalDate tradingDate, List<String> states);

    List<StockJpaEntity> findByTradingDateOrderByGapPercentDesc(LocalDate tradingDate);

    @Query("SELECT s FROM StockJpaEntity s WHERE s.tradingDate = :tradingDate " +
           "AND s.state IN ('WATCHING', 'HIGH_FORMED', 'PULLBACK', 'ENTRY_READY')")
    List<StockJpaEntity> findActiveStocks(@Param("tradingDate") LocalDate tradingDate);

    int countByTradingDateAndState(LocalDate tradingDate, String state);

    @Modifying
    @Query("DELETE FROM StockJpaEntity s WHERE s.tradingDate < :date")
    void deleteByTradingDateBefore(@Param("date") LocalDate date);
}
