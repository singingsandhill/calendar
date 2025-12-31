package me.singingsandhill.calendar.trading.infrastructure.persistence.repository;

import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.TradeJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeJpaRepository extends JpaRepository<TradeJpaEntity, Long> {

    Optional<TradeJpaEntity> findByUuid(String uuid);

    @Query("SELECT t FROM TradeJpaEntity t WHERE t.market = :market ORDER BY t.createdAt DESC")
    List<TradeJpaEntity> findByMarketOrderByCreatedAtDesc(@Param("market") String market, Pageable pageable);

    List<TradeJpaEntity> findByStatus(String status);

    List<TradeJpaEntity> findByPositionId(Long positionId);

    @Query("SELECT t FROM TradeJpaEntity t WHERE t.market = :market AND t.createdAt BETWEEN :start AND :end")
    List<TradeJpaEntity> findByMarketAndCreatedAtBetween(
            @Param("market") String market,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    long countByMarketAndStatus(String market, String status);
}
