package me.singingsandhill.calendar.stock.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.stock.domain.position.StockCloseReason;
import me.singingsandhill.calendar.stock.domain.trade.*;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
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
    private final StockProperties stockProperties;

    public StockTradeRepositoryAdapter(StockTradeJpaRepository jpaRepository, StockProperties stockProperties) {
        this.jpaRepository = jpaRepository;
        this.stockProperties = stockProperties;
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
        entity.setOrderedAt(trade.getOrderedAt());
        entity.setCreatedAt(trade.getCreatedAt());
        // insert 시점의 봇 모드를 찍는다 — 모드는 프로세스 수명 동안 바뀌지 않는다 (ADR stock/observability/0003)
        entity.setMode(trade.getMode() != null ? trade.getMode().name() : stockProperties.getBot().getMode().name());
        updateEntity(entity, trade);
        return entity;
    }

    private void updateEntity(StockTradeJpaEntity entity, StockTrade trade) {
        // 선영속화(PENDING-) 뒤 assignBrokerOrderId 로 바뀐 주문번호 — 빠지면 DB 의 매수 주문번호가 영원히
        // PENDING- 로 남아 가상 예수금 원장·미확인 주문 스윕이 체결 매수를 알아보지 못한다 (ADR stock/modes/0004).
        entity.setOrderId(trade.getOrderId());
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

    /**
     * 저장된 값을 그대로 복원한다. 팩토리 + markFilled 로 복원하면 시각이 조회 시각이 되고 CANCELLED 가 PENDING 으로
     * 되살아났다 (ADR stock/observability/0003).
     */
    private StockTrade toDomain(StockTradeJpaEntity entity) {
        return StockTrade.restore(
            entity.getId(),
            entity.getOrderId(),
            entity.getPositionId(),
            entity.getStockCode(),
            StockTradeType.valueOf(entity.getTradeType()),
            StockOrderType.valueOf(entity.getOrderType()),
            entity.getQuantity(),
            entity.getOrderPrice(),
            entity.getExecutedQuantity(),
            entity.getExecutedPrice(),
            entity.getFee(),
            entity.getStatus() != null ? StockTradeStatus.valueOf(entity.getStatus()) : null,
            entity.getExitReason() != null ? StockCloseReason.valueOf(entity.getExitReason()) : null,
            entity.getOrderedAt(),
            entity.getExecutedAt(),
            entity.getCreatedAt(),
            StockTradeMode.fromName(entity.getMode())
        );
    }
}
