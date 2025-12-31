package me.singingsandhill.calendar.stock.infrastructure.persistence.repository;

import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockPositionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockPositionJpaRepository extends JpaRepository<StockPositionJpaEntity, Long> {

    Optional<StockPositionJpaEntity> findByStockCodeAndTradingDateAndStatusNot(
        String stockCode, LocalDate tradingDate, String status);

    List<StockPositionJpaEntity> findByTradingDate(LocalDate tradingDate);

    List<StockPositionJpaEntity> findByTradingDateAndStatus(LocalDate tradingDate, String status);

    @Query("SELECT p FROM StockPositionJpaEntity p WHERE p.tradingDate = :tradingDate " +
           "AND p.status IN ('OPEN', 'PARTIAL')")
    List<StockPositionJpaEntity> findOpenPositions(@Param("tradingDate") LocalDate tradingDate);

    @Query("SELECT p FROM StockPositionJpaEntity p WHERE p.tradingDate = :tradingDate " +
           "AND p.status = 'CLOSED'")
    List<StockPositionJpaEntity> findClosedPositions(@Param("tradingDate") LocalDate tradingDate);

    @Query("SELECT COUNT(p) FROM StockPositionJpaEntity p WHERE p.tradingDate = :tradingDate " +
           "AND p.status IN ('OPEN', 'PARTIAL')")
    int countOpenPositions(@Param("tradingDate") LocalDate tradingDate);

    @Modifying
    @Query("DELETE FROM StockPositionJpaEntity p WHERE p.tradingDate < :date")
    void deleteByTradingDateBefore(@Param("date") LocalDate date);
}
