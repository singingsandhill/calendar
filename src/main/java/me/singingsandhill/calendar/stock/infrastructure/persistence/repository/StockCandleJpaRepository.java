package me.singingsandhill.calendar.stock.infrastructure.persistence.repository;

import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockCandleJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockCandleJpaRepository extends JpaRepository<StockCandleJpaEntity, Long> {

    Optional<StockCandleJpaEntity> findByStockCodeAndCandleDatetimeAndIntervalType(
        String stockCode, LocalDateTime candleDatetime, String intervalType);

    @Query("SELECT c FROM StockCandleJpaEntity c " +
           "WHERE c.stockCode = :stockCode AND c.intervalType = :intervalType " +
           "ORDER BY c.candleDatetime DESC")
    List<StockCandleJpaEntity> findByStockCodeAndIntervalTypeOrderByDatetimeDesc(
        @Param("stockCode") String stockCode,
        @Param("intervalType") String intervalType,
        Pageable pageable);

    @Query("SELECT c FROM StockCandleJpaEntity c " +
           "WHERE c.stockCode = :stockCode AND c.intervalType = :intervalType " +
           "ORDER BY c.candleDatetime DESC LIMIT 1")
    Optional<StockCandleJpaEntity> findLatestByStockCodeAndIntervalType(
        @Param("stockCode") String stockCode,
        @Param("intervalType") String intervalType);

    @Query("SELECT c FROM StockCandleJpaEntity c " +
           "WHERE c.stockCode = :stockCode AND c.intervalType = :intervalType " +
           "AND c.candleDatetime BETWEEN :from AND :to " +
           "ORDER BY c.candleDatetime DESC")
    List<StockCandleJpaEntity> findByStockCodeAndIntervalTypeAndDatetimeRange(
        @Param("stockCode") String stockCode,
        @Param("intervalType") String intervalType,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to);

    @Modifying
    @Query("DELETE FROM StockCandleJpaEntity c WHERE c.candleDatetime < :dateTime")
    void deleteByDatetimeBefore(@Param("dateTime") LocalDateTime dateTime);
}
