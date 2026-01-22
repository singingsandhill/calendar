package me.singingsandhill.calendar.trading.infrastructure.persistence.repository;

import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.CandleJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CandleJpaRepository extends JpaRepository<CandleJpaEntity, Long> {

    @Query("SELECT c FROM CandleJpaEntity c WHERE c.market = :market ORDER BY c.candleDateTime DESC")
    List<CandleJpaEntity> findByMarketOrderByDateTimeDesc(@Param("market") String market, Pageable pageable);

    @Query("SELECT c FROM CandleJpaEntity c WHERE c.market = :market ORDER BY c.candleDateTime DESC LIMIT 1")
    Optional<CandleJpaEntity> findLatestByMarket(@Param("market") String market);

    Optional<CandleJpaEntity> findByMarketAndCandleDateTime(String market, LocalDateTime candleDateTime);

    List<CandleJpaEntity> findByMarketAndCandleDateTimeBetween(String market, LocalDateTime from, LocalDateTime to);

    @Modifying
    @Query("DELETE FROM CandleJpaEntity c WHERE c.createdAt < :dateTime")
    void deleteOlderThan(@Param("dateTime") LocalDateTime dateTime);

    @Modifying
    @Query("DELETE FROM CandleJpaEntity c WHERE c.candleDateTime < :dateTime")
    int deleteByCandleDateTimeBefore(@Param("dateTime") LocalDateTime dateTime);

    long countByMarket(String market);

    /**
     * 배치 조회: 주어진 시간 목록 중 이미 존재하는 캔들의 시간만 반환
     * N+1 쿼리 문제 해결을 위한 메서드
     */
    @Query("SELECT c.candleDateTime FROM CandleJpaEntity c WHERE c.market = :market AND c.candleDateTime IN :dateTimes")
    Set<LocalDateTime> findCandleDateTimesByMarketAndDateTimeIn(
            @Param("market") String market,
            @Param("dateTimes") Collection<LocalDateTime> dateTimes);
}
