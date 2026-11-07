package me.singingsandhill.calendar.stock.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.position.StockPositionRepository;
import me.singingsandhill.calendar.stock.domain.position.StockPositionStatus;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeMode;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
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
    private final StockProperties stockProperties;

    public StockPositionRepositoryAdapter(StockPositionJpaRepository jpaRepository, StockProperties stockProperties) {
        this.jpaRepository = jpaRepository;
        this.stockProperties = stockProperties;
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
    public List<StockPosition> findByTradingDateBetween(LocalDate from, LocalDate to) {
        return jpaRepository.findByTradingDateBetweenOrderByTradingDateDescEnteredAtDesc(from, to)
            .stream()
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
        entity.setCreatedAt(position.getCreatedAt());
        // insert 시점의 봇 모드를 찍는다 (ADR stock/observability/0003)
        entity.setMode(position.getMode() != null ? position.getMode().name()
            : stockProperties.getBot().getMode().name());
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
        // 누적 청산 — 빠지면 재로딩 후 평균 청산가·실현손익률이 마지막 청산분만으로 계산된다
        entity.setTotalExitAmount(position.getTotalExitAmount());
        entity.setTotalExitQuantity(position.getTotalExitQuantity());
        if (position.getCloseReason() != null) {
            entity.setCloseReason(position.getCloseReason().name());
        }
        entity.setClosedAt(position.getClosedAt());
    }

    private StockPosition toDomain(StockPositionJpaEntity entity) {
        StockCloseReason closeReason = entity.getCloseReason() != null
            ? StockCloseReason.valueOf(entity.getCloseReason())
            : null;
        StockPositionStatus status = entity.getStatus() != null
            ? StockPositionStatus.valueOf(entity.getStatus())
            : StockPositionStatus.OPEN;
        return StockPosition.reconstitute(
            entity.getId(),
            entity.getStockId(),
            entity.getStockCode(),
            entity.getTradingDate(),
            status,
            entity.getEntryPrice(),
            entity.getEntryQuantity(),
            entity.getEntryAmount(),
            entity.getEnteredAt(),
            entity.getRemainingQuantity(),
            entity.getAverageExitPrice(),
            Boolean.TRUE.equals(entity.getTp1Executed()),
            Boolean.TRUE.equals(entity.getTp2Executed()),
            Boolean.TRUE.equals(entity.getTp3Executed()),
            entity.getDayHighPrice(),
            entity.getStopLossPrice(),
            entity.getTrailingHigh(),
            entity.getTrailingStopPrice(),
            Boolean.TRUE.equals(entity.getTrailingActive()),
            entity.getRealizedPnl(),
            entity.getRealizedPnlPercent(),
            entity.getTotalExitAmount(),
            entity.getTotalExitQuantity(),
            closeReason,
            entity.getClosedAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            StockTradeMode.fromName(entity.getMode())
        );
    }
}
