package me.singingsandhill.calendar.trading.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.trading.domain.position.CloseReason;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionRepository;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.PositionJpaEntity;
import me.singingsandhill.calendar.trading.infrastructure.persistence.repository.PositionJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class PositionRepositoryAdapter implements PositionRepository {

    private final PositionJpaRepository jpaRepository;

    public PositionRepositoryAdapter(PositionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Position save(Position position) {
        PositionJpaEntity entity = toEntity(position);
        PositionJpaEntity saved = jpaRepository.save(entity);
        position.setId(saved.getId());
        return position;
    }

    @Override
    public Optional<Position> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Position> findOpenPositionByMarket(String market) {
        return jpaRepository.findOpenPositionByMarket(market).map(this::toDomain);
    }

    @Override
    public List<Position> findByMarketOrderByOpenedAtDesc(String market, int page, int size) {
        return jpaRepository.findByMarketOrderByOpenedAtDesc(market, PageRequest.of(page, size))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Position> findByStatus(PositionStatus status) {
        return jpaRepository.findByStatus(status.name())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Position> findByMarketAndStatus(String market, PositionStatus status) {
        return jpaRepository.findByMarketAndStatus(market, status.name())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Position> findByMarketAndClosedAtBetween(String market, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByMarketAndClosedAtBetween(market, start, end)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Position> findByMarketAndStatusAndClosedAtBetween(String market, PositionStatus status,
                                                                    LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByMarketAndStatusAndClosedAtBetween(market, status.name(), start, end)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByMarketAndStatus(String market, PositionStatus status) {
        return jpaRepository.countByMarketAndStatus(market, status.name());
    }

    private PositionJpaEntity toEntity(Position position) {
        PositionJpaEntity entity = new PositionJpaEntity(
                position.getMarket(),
                position.getStatus().name(),
                position.getEntryPrice(),
                position.getEntryVolume(),
                position.getEntryAmount(),
                position.getExitPrice(),
                position.getExitVolume(),
                position.getExitAmount(),
                position.getRealizedPnl(),
                position.getRealizedPnlPct(),
                position.getStopLossPrice(),
                position.getTakeProfitPrice(),
                position.getTrailingStopPrice(),
                position.getHighWaterMark(),
                position.isTrailingStopActive(),
                position.getCloseReason() != null ? position.getCloseReason().name() : null,
                position.getOpenedAt(),
                position.getClosedAt(),
                position.getCreatedAt(),
                position.getEntryFee(),
                position.getExitFee(),
                position.getTotalFees()
        );
        if (position.getId() != null) {
            entity.setId(position.getId());
        }
        // Issue #5: 청산 시도 추적 필드 매핑
        entity.setClosingAttempted(position.isClosingAttempted());
        entity.setLastCloseAttemptAt(position.getLastCloseAttemptAt());
        entity.setCloseAttemptCount(position.getCloseAttemptCount());
        return entity;
    }

    private Position toDomain(PositionJpaEntity entity) {
        Position position = new Position(
                entity.getId(),
                entity.getMarket(),
                PositionStatus.valueOf(entity.getStatus()),
                entity.getEntryPrice(),
                entity.getEntryVolume(),
                entity.getEntryAmount(),
                entity.getExitPrice(),
                entity.getExitVolume(),
                entity.getExitAmount(),
                entity.getRealizedPnl(),
                entity.getRealizedPnlPct(),
                entity.getStopLossPrice(),
                entity.getTakeProfitPrice(),
                entity.getTrailingStopPrice(),
                entity.getHighWaterMark(),
                entity.isTrailingStopActive(),
                entity.getCloseReason() != null ? CloseReason.valueOf(entity.getCloseReason()) : null,
                entity.getOpenedAt(),
                entity.getClosedAt(),
                entity.getCreatedAt(),
                entity.getEntryFee(),
                entity.getExitFee(),
                entity.getTotalFees()
        );
        // Issue #5: 청산 시도 추적 필드 복원
        position.setClosingAttempted(entity.isClosingAttempted() != null && entity.isClosingAttempted());
        position.setLastCloseAttemptAt(entity.getLastCloseAttemptAt());
        position.setCloseAttemptCount(entity.getCloseAttemptCount() != null ? entity.getCloseAttemptCount() : 0);
        return position;
    }
}
