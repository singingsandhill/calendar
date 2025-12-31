package me.singingsandhill.calendar.stock.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.trade.*;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockTradeJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockTradeJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class StockTradeRepositoryAdapter implements StockTradeRepository {

    private final StockTradeJpaRepository jpaRepository;

    public StockTradeRepositoryAdapter(StockTradeJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public StockTrade save(StockTrade trade) {
        StockTradeJpaEntity entity;

        if (trade.getId() != null) {
            entity = jpaRepository.findById(trade.getId())
                .orElseThrow(() -> new IllegalStateException("Trade not found: " + trade.getId()));
            updateEntity(entity, trade);
        } else {
            entity = toEntity(trade);
        }

        StockTradeJpaEntity saved = jpaRepository.save(entity);
        StockTrade result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public Optional<StockTrade> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<StockTrade> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId).map(this::toDomain);
    }

    @Override
    public List<StockTrade> findByPositionId(Long positionId) {
        return jpaRepository.findByPositionId(positionId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockTrade> findByStockCodeAndOrderedAtBetween(
            String stockCode, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByStockCodeAndOrderedAtBetween(stockCode, from, to).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockTrade> findByOrderedAtBetween(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByOrderedAtBetween(from, to).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockTrade> findTodayTrades() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return jpaRepository.findTodayTrades(startOfDay).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByOrderedAtBefore(LocalDateTime dateTime) {
        jpaRepository.deleteByOrderedAtBefore(dateTime);
    }

    private StockTradeJpaEntity toEntity(StockTrade trade) {
        StockTradeJpaEntity entity = new StockTradeJpaEntity(
            trade.getOrderId(),
            trade.getStockCode(),
            trade.getTradeType().name(),
            trade.getOrderType().name(),
            trade.getQuantity(),
            trade.getOrderPrice()
        );
        updateEntity(entity, trade);
        return entity;
    }

    private void updateEntity(StockTradeJpaEntity entity, StockTrade trade) {
        entity.setPositionId(trade.getPositionId());
        entity.setExecutedQuantity(trade.getExecutedQuantity());
        entity.setExecutedPrice(trade.getExecutedPrice());
        entity.setFee(trade.getFee());
        entity.setStatus(trade.getStatus().name());
        if (trade.getExitReason() != null) {
            entity.setExitReason(trade.getExitReason().name());
        }
        entity.setExecutedAt(trade.getExecutedAt());
    }

    private StockTrade toDomain(StockTradeJpaEntity entity) {
        StockTrade trade;
        if ("BUY".equals(entity.getTradeType())) {
            trade = StockTrade.createBuyOrder(
                entity.getOrderId(),
                entity.getStockCode(),
                entity.getQuantity(),
                entity.getOrderPrice(),
                "MARKET".equals(entity.getOrderType())
            );
        } else {
            StockCloseReason reason = entity.getExitReason() != null
                ? StockCloseReason.valueOf(entity.getExitReason())
                : null;
            trade = StockTrade.createSellOrder(
                entity.getOrderId(),
                entity.getStockCode(),
                entity.getQuantity(),
                entity.getOrderPrice(),
                "MARKET".equals(entity.getOrderType()),
                reason
            );
        }
        trade.setId(entity.getId());
        trade.setPositionId(entity.getPositionId());

        if (entity.getExecutedQuantity() != null && entity.getExecutedQuantity() > 0) {
            trade.markFilled(entity.getExecutedPrice(), entity.getExecutedQuantity(), entity.getFee());
        }

        return trade;
    }
}
