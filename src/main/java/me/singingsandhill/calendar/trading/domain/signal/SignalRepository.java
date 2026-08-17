package me.singingsandhill.calendar.trading.domain.signal;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SignalRepository {

    Signal save(Signal signal);

    Optional<Signal> findById(Long id);

    List<Signal> findByMarketOrderBySignalTimeDesc(String market, int limit);

    Optional<Signal> findLatestByMarket(String market);

    List<Signal> findByMarketAndSignalType(String market, SignalType signalType, int limit);

    List<Signal> findByMarketAndSignalTimeBetween(String market, LocalDateTime start, LocalDateTime end);

    long countByMarketAndExecuted(String market, boolean executed);

    /**
     * 분석용 축소 관측치를 시간 오름차순으로 조회한다.
     * 엔티티가 아니라 투영이라 대구간(90일 ≈ 130,000행)에서도 힙에 남지 않는다.
     */
    List<SignalSample> findSamplesByMarketAndSignalTimeBetween(String market, LocalDateTime start, LocalDateTime end);
}
