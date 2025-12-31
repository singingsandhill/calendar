package me.singingsandhill.calendar.trading.domain.trade;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TradeRepository {

    Trade save(Trade trade);

    Optional<Trade> findById(Long id);

    Optional<Trade> findByUuid(String uuid);

    List<Trade> findByMarketOrderByCreatedAtDesc(String market, int page, int size);

    List<Trade> findByStatus(TradeStatus status);

    List<Trade> findByPositionId(Long positionId);

    List<Trade> findByMarketAndCreatedAtBetween(String market, LocalDateTime start, LocalDateTime end);

    long countByMarketAndStatus(String market, TradeStatus status);
}
