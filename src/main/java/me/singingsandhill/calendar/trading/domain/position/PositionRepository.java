package me.singingsandhill.calendar.trading.domain.position;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PositionRepository {

    Position save(Position position);

    Optional<Position> findById(Long id);

    List<Position> findOpenPositionsByMarket(String market);

    List<Position> findByMarketOrderByOpenedAtDesc(String market, int page, int size);

    List<Position> findByStatus(PositionStatus status);

    List<Position> findByMarketAndStatus(String market, PositionStatus status);

    List<Position> findByMarketAndClosedAtBetween(String market, LocalDateTime start, LocalDateTime end);

    List<Position> findByMarketAndStatusAndClosedAtBetween(String market, PositionStatus status,
                                                            LocalDateTime start, LocalDateTime end);

    long countByMarketAndStatus(String market, PositionStatus status);

    /** 해당 사유로 가장 최근에 청산된 포지션 — 비상 손절 뒤 리밸런싱 재매수 차단을 재시작 후에도 복원 (ADR risk/0007). */
    Optional<Position> findLatestClosedByMarketAndCloseReason(String market, CloseReason closeReason);
}
