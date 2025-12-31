package me.singingsandhill.calendar.trading.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.trading.domain.trade.Trade;
import me.singingsandhill.calendar.trading.domain.trade.TradeRepository;
import me.singingsandhill.calendar.trading.domain.trade.TradeStatus;
import me.singingsandhill.calendar.trading.domain.trade.TradeType;
import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.TradeJpaEntity;
import me.singingsandhill.calendar.trading.infrastructure.persistence.repository.TradeJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class TradeRepositoryAdapter implements TradeRepository {

    private final TradeJpaRepository jpaRepository;

    public TradeRepositoryAdapter(TradeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Trade save(Trade trade) {
        TradeJpaEntity entity = toEntity(trade);
        TradeJpaEntity saved = jpaRepository.save(entity);
        trade.setId(saved.getId());
        return trade;
    }

    @Override
    public Optional<Trade> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Trade> findByUuid(String uuid) {
        return jpaRepository.findByUuid(uuid).map(this::toDomain);
    }

    @Override
    public List<Trade> findByMarketOrderByCreatedAtDesc(String market, int page, int size) {
        return jpaRepository.findByMarketOrderByCreatedAtDesc(market, PageRequest.of(page, size))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Trade> findByStatus(TradeStatus status) {
        return jpaRepository.findByStatus(status.name())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Trade> findByPositionId(Long positionId) {
        return jpaRepository.findByPositionId(positionId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Trade> findByMarketAndCreatedAtBetween(String market, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByMarketAndCreatedAtBetween(market, start, end)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByMarketAndStatus(String market, TradeStatus status) {
        return jpaRepository.countByMarketAndStatus(market, status.name());
    }

    private TradeJpaEntity toEntity(Trade trade) {
        TradeJpaEntity entity = new TradeJpaEntity(
                trade.getUuid(),
                trade.getPositionId(),
                trade.getMarket(),
                trade.getTradeType().name(),
                trade.getOrderType(),
                trade.getPrice(),
                trade.getVolume(),
                trade.getExecutedPrice(),
                trade.getExecutedVolume(),
                trade.getFee(),
                trade.getStatus().name(),
                trade.getSignalScore(),
                trade.getSignalReason(),
                trade.getOrderedAt(),
                trade.getExecutedAt(),
                trade.getCreatedAt()
        );
        if (trade.getId() != null) {
            entity.setId(trade.getId());
        }
        return entity;
    }

    private Trade toDomain(TradeJpaEntity entity) {
        return new Trade(
                entity.getId(),
                entity.getUuid(),
                entity.getPositionId(),
                entity.getMarket(),
                TradeType.valueOf(entity.getTradeType()),
                entity.getOrderType(),
                entity.getPrice(),
                entity.getVolume(),
                entity.getExecutedPrice(),
                entity.getExecutedVolume(),
                entity.getFee(),
                TradeStatus.valueOf(entity.getStatus()),
                entity.getSignalScore(),
                entity.getSignalReason(),
                entity.getOrderedAt(),
                entity.getExecutedAt(),
                entity.getCreatedAt()
        );
    }
}
