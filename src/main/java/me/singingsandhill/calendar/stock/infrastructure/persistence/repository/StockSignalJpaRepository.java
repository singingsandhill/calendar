package me.singingsandhill.calendar.stock.infrastructure.persistence.repository;

import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockSignalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface StockSignalJpaRepository extends JpaRepository<StockSignalJpaEntity, Long> {

    List<StockSignalJpaEntity> findByStockCodeAndSignalTimeBetween(
        String stockCode, LocalDateTime from, LocalDateTime to);

    List<StockSignalJpaEntity> findBySignalTimeBetween(LocalDateTime from, LocalDateTime to);

    List<StockSignalJpaEntity> findByStockCodeAndSignalType(String stockCode, String signalType);

    @Query("SELECT s FROM StockSignalJpaEntity s WHERE s.signalTime >= :startOfDay ORDER BY s.signalTime DESC")
    List<StockSignalJpaEntity> findTodaySignals(@Param("startOfDay") LocalDateTime startOfDay);

    @Modifying
    @Query("DELETE FROM StockSignalJpaEntity s WHERE s.signalTime < :dateTime")
    void deleteBySignalTimeBefore(@Param("dateTime") LocalDateTime dateTime);
}
