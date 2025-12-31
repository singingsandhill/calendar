package me.singingsandhill.calendar.trading.domain.position;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PositionRepository {

    Position save(Position position);

    Optional<Position> findById(Long id);

    Optional<Position> findOpenPositionByMarket(String market);

    List<Position> findByMarketOrderByOpenedAtDesc(String market, int page, int size);

    List<Position> findByStatus(PositionStatus status);

    List<Position> findByMarketAndStatus(String market, PositionStatus status);

    List<Position> findByMarketAndClosedAtBetween(String market, LocalDateTime start, LocalDateTime end);

    List<Position> findByMarketAndStatusAndClosedAtBetween(String market, PositionStatus status,
                                                            LocalDateTime start, LocalDateTime end);

    long countByMarketAndStatus(String market, PositionStatus status);
}
