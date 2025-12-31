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
}
