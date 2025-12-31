package me.singingsandhill.calendar.trading.infrastructure.persistence.repository;

import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.PositionJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PositionJpaRepository extends JpaRepository<PositionJpaEntity, Long> {

    @Query("SELECT p FROM PositionJpaEntity p WHERE p.market = :market AND p.status = 'OPEN'")
    Optional<PositionJpaEntity> findOpenPositionByMarket(@Param("market") String market);

    @Query("SELECT p FROM PositionJpaEntity p WHERE p.market = :market ORDER BY p.openedAt DESC")
    List<PositionJpaEntity> findByMarketOrderByOpenedAtDesc(@Param("market") String market, Pageable pageable);

    List<PositionJpaEntity> findByStatus(String status);

    List<PositionJpaEntity> findByMarketAndStatus(String market, String status);

    @Query("SELECT p FROM PositionJpaEntity p WHERE p.market = :market AND p.closedAt BETWEEN :start AND :end")
    List<PositionJpaEntity> findByMarketAndClosedAtBetween(
            @Param("market") String market,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT p FROM PositionJpaEntity p WHERE p.market = :market AND p.status = :status AND p.closedAt BETWEEN :start AND :end")
    List<PositionJpaEntity> findByMarketAndStatusAndClosedAtBetween(
            @Param("market") String market,
            @Param("status") String status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    long countByMarketAndStatus(String market, String status);
}
