package me.singingsandhill.calendar.stock.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.domain.position.StockPositionStatus;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockPositionJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockPositionJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class StockPositionRepositoryAdapter implements StockPositionRepository {

    private final StockPositionJpaRepository jpaRepository;

    public StockPositionRepositoryAdapter(StockPositionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public StockPosition save(StockPosition position) {
        StockPositionJpaEntity entity;

        if (position.getId() != null) {
            entity = jpaRepository.findById(position.getId())
                .orElseThrow(() -> new IllegalStateException("Position not found: " + position.getId()));
            updateEntity(entity, position);
        } else {
            entity = toEntity(position);
        }

        StockPositionJpaEntity saved = jpaRepository.save(entity);
        StockPosition result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public Optional<StockPosition> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<StockPosition> findByStockCodeAndTradingDateAndStatusNot(
            String stockCode, LocalDate tradingDate, StockPositionStatus status) {
        return jpaRepository.findByStockCodeAndTradingDateAndStatusNot(
            stockCode, tradingDate, status.name()).map(this::toDomain);
    }

    @Override
    public List<StockPosition> findByTradingDate(LocalDate tradingDate) {
        return jpaRepository.findByTradingDate(tradingDate).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockPosition> findByTradingDateAndStatus(LocalDate tradingDate, StockPositionStatus status) {
        return jpaRepository.findByTradingDateAndStatus(tradingDate, status.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockPosition> findOpenPositions(LocalDate tradingDate) {
        return jpaRepository.findOpenPositions(tradingDate).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockPosition> findClosedPositions(LocalDate tradingDate) {
        return jpaRepository.findClosedPositions(tradingDate).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public int countOpenPositions(LocalDate tradingDate) {
        return jpaRepository.countOpenPositions(tradingDate);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByTradingDateBefore(LocalDate date) {
        jpaRepository.deleteByTradingDateBefore(date);
    }

    private StockPositionJpaEntity toEntity(StockPosition position) {
        StockPositionJpaEntity entity = new StockPositionJpaEntity(
            position.getStockCode(),
            position.getTradingDate(),
            position.getStatus().name()
        );
        updateEntity(entity, position);
        return entity;
    }

    private void updateEntity(StockPositionJpaEntity entity, StockPosition position) {
        entity.setStockId(position.getStockId());
        entity.setStatus(position.getStatus().name());
        entity.setEntryPrice(position.getEntryPrice());
        entity.setEntryQuantity(position.getEntryQuantity());
        entity.setEntryAmount(position.getEntryAmount());
        entity.setEnteredAt(position.getEnteredAt());
        entity.setRemainingQuantity(position.getRemainingQuantity());
        entity.setAverageExitPrice(position.getAverageExitPrice());
        entity.setTp1Executed(position.isTp1Executed());
        entity.setTp2Executed(position.isTp2Executed());
        entity.setTp3Executed(position.isTp3Executed());
        entity.setDayHighPrice(position.getDayHighPrice());
        entity.setStopLossPrice(position.getStopLossPrice());
        entity.setTrailingHigh(position.getTrailingHigh());
        entity.setTrailingStopPrice(position.getTrailingStopPrice());
        entity.setTrailingActive(position.isTrailingActive());
        entity.setRealizedPnl(position.getRealizedPnl());
        entity.setRealizedPnlPercent(position.getRealizedPnlPercent());
        if (position.getCloseReason() != null) {
            entity.setCloseReason(position.getCloseReason().name());
        }
        entity.setClosedAt(position.getClosedAt());
    }

    private StockPosition toDomain(StockPositionJpaEntity entity) {
        StockPosition position = StockPosition.open(
            entity.getStockCode(),
            entity.getTradingDate(),
            entity.getEntryPrice(),
            entity.getEntryQuantity(),
            entity.getStopLossPrice(),
            entity.getDayHighPrice()
        );
        position.setId(entity.getId());
        position.setStockId(entity.getStockId());
        return position;
    }
}
