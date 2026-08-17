package me.singingsandhill.calendar.trading.infrastructure.persistence.repository;

import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.SignalJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    /**
     * 분석용 축소 투영. 엔티티가 아니라 인터페이스 프로젝션이라 영속성 컨텍스트에 남지 않는다 —
     * 90일 구간이 ~130,000행이라 엔티티로 읽으면 요청이 끝날 때까지 힙을 붙잡는다.
     *
     * <p>정렬을 SQL 로 내려 서비스 쪽 전방수익 계산이 단순 2-포인터 스캔이 되게 한다.
     */
    @Query("""
            SELECT s.id AS id, s.signalTime AS signalTime, s.signalType AS signalType,
                   s.totalScore AS totalScore,
                   s.maCrossScore AS maCrossScore, s.maTrendScore AS maTrendScore,
                   s.rsiDivergenceScore AS rsiDivergenceScore, s.rsiLevelScore AS rsiLevelScore,
                   s.stochDivergenceScore AS stochDivergenceScore, s.stochLevelScore AS stochLevelScore,
                   s.volumeDivergenceScore AS volumeDivergenceScore, s.rsiTrendScore AS rsiTrendScore,
                   s.ma60 AS ma60, s.rsi AS rsi, s.stochK AS stochK, s.stochD AS stochD,
                   s.rsiDivergence AS rsiDivergence, s.stochDivergence AS stochDivergence,
                   s.volumeDivergence AS volumeDivergence,
                   s.currentPrice AS currentPrice,
                   s.volumeMa AS volumeMa, s.currentVolume AS currentVolume, s.atrPercent AS atrPercent
              FROM SignalJpaEntity s
             WHERE s.market = :market AND s.signalTime BETWEEN :start AND :end
             ORDER BY s.signalTime ASC
            """)
    List<SignalSampleView> findSamples(@Param("market") String market,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    /** enum 은 String 으로 저장돼 있어 그대로 받고, 어댑터에서 도메인 타입으로 변환한다. */
    interface SignalSampleView {
        Long getId();
        LocalDateTime getSignalTime();
        String getSignalType();
        Integer getTotalScore();
        Integer getMaCrossScore();
        Integer getMaTrendScore();
        Integer getRsiDivergenceScore();
        Integer getRsiLevelScore();
        Integer getStochDivergenceScore();
        Integer getStochLevelScore();
        Integer getVolumeDivergenceScore();
        Integer getRsiTrendScore();
        BigDecimal getMa60();
        BigDecimal getRsi();
        BigDecimal getStochK();
        BigDecimal getStochD();
        String getRsiDivergence();
        String getStochDivergence();
        String getVolumeDivergence();
        BigDecimal getCurrentPrice();
        BigDecimal getVolumeMa();
        BigDecimal getCurrentVolume();
        BigDecimal getAtrPercent();
    }
}
