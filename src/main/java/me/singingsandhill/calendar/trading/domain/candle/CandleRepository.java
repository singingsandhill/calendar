package me.singingsandhill.calendar.trading.domain.candle;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CandleRepository {

    Candle save(Candle candle);

    List<Candle> saveAll(List<Candle> candles);

    Optional<Candle> findById(Long id);

    List<Candle> findByMarketOrderByDateTimeDesc(String market, int limit);

    Optional<Candle> findLatestByMarket(String market);

    Optional<Candle> findByMarketAndDateTime(String market, LocalDateTime dateTime);

    Optional<Candle> findByMarketAndCandleDateTime(String market, LocalDateTime candleDateTime);

    List<Candle> findByMarketAndDateTimeRange(String market, LocalDateTime from, LocalDateTime to);

    void deleteOlderThan(LocalDateTime dateTime);

    int deleteByDateTimeBefore(LocalDateTime dateTime);

    long countByMarket(String market);

    /**
     * 배치 조회: 주어진 시간 목록 중 이미 존재하는 캔들의 시간만 반환
     * N+1 쿼리 문제 해결을 위한 메서드
     */
    Set<LocalDateTime> findExistingDateTimesByMarketAndDateTimeIn(String market, Collection<LocalDateTime> dateTimes);
}
