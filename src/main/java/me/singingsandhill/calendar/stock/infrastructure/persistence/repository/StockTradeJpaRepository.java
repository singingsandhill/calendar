package me.singingsandhill.calendar.stock.infrastructure.persistence.repository;

import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockTradeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockTradeJpaRepository extends JpaRepository<StockTradeJpaEntity, Long> {

    Optional<StockTradeJpaEntity> findByOrderId(String orderId);

    List<StockTradeJpaEntity> findByPositionId(Long positionId);

    List<StockTradeJpaEntity> findByStockCodeAndOrderedAtBetween(
        String stockCode, LocalDateTime from, LocalDateTime to);

    List<StockTradeJpaEntity> findByOrderedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT t FROM StockTradeJpaEntity t WHERE t.orderedAt >= :startOfDay ORDER BY t.orderedAt DESC")
    List<StockTradeJpaEntity> findTodayTrades(@Param("startOfDay") LocalDateTime startOfDay);

    @Modifying
    @Query("DELETE FROM StockTradeJpaEntity t WHERE t.orderedAt < :dateTime")
    void deleteByOrderedAtBefore(@Param("dateTime") LocalDateTime dateTime);
}
