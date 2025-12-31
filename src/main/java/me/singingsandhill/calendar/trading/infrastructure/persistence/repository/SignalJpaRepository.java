package me.singingsandhill.calendar.trading.infrastructure.persistence.repository;

import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.SignalJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SignalJpaRepository extends JpaRepository<SignalJpaEntity, Long> {

    @Query("SELECT s FROM SignalJpaEntity s WHERE s.market = :market ORDER BY s.signalTime DESC")
    List<SignalJpaEntity> findByMarketOrderBySignalTimeDesc(@Param("market") String market, Pageable pageable);

    @Query("SELECT s FROM SignalJpaEntity s WHERE s.market = :market ORDER BY s.signalTime DESC LIMIT 1")
    Optional<SignalJpaEntity> findLatestByMarket(@Param("market") String market);

    @Query("SELECT s FROM SignalJpaEntity s WHERE s.market = :market AND s.signalType = :signalType ORDER BY s.signalTime DESC")
    List<SignalJpaEntity> findByMarketAndSignalType(
            @Param("market") String market,
            @Param("signalType") String signalType,
            Pageable pageable);

    @Query("SELECT s FROM SignalJpaEntity s WHERE s.market = :market AND s.signalTime BETWEEN :start AND :end")
    List<SignalJpaEntity> findByMarketAndSignalTimeBetween(
            @Param("market") String market,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    long countByMarketAndExecuted(String market, boolean executed);
}
