package me.singingsandhill.calendar.stock.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.stock.domain.signal.StockSignal;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalType;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockSignalJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockSignalJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class StockSignalRepositoryAdapter implements StockSignalRepository {

    private final StockSignalJpaRepository jpaRepository;

    public StockSignalRepositoryAdapter(StockSignalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public StockSignal save(StockSignal signal) {
        StockSignalJpaEntity entity = toEntity(signal);
        StockSignalJpaEntity saved = jpaRepository.save(entity);
        StockSignal result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public Optional<StockSignal> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<StockSignal> findByStockCodeAndSignalTimeBetween(
            String stockCode, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByStockCodeAndSignalTimeBetween(stockCode, from, to).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockSignal> findBySignalTimeBetween(LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findBySignalTimeBetween(from, to).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockSignal> findByStockCodeAndSignalType(String stockCode, StockSignalType signalType) {
        return jpaRepository.findByStockCodeAndSignalType(stockCode, signalType.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<StockSignal> findTodaySignals() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return jpaRepository.findTodaySignals(startOfDay).stream()
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
    public void deleteBySignalTimeBefore(LocalDateTime dateTime) {
        jpaRepository.deleteBySignalTimeBefore(dateTime);
    }

    private StockSignalJpaEntity toEntity(StockSignal signal) {
        StockSignalJpaEntity entity = new StockSignalJpaEntity(
            signal.getStockCode(),
            signal.getSignalTime(),
            signal.getSignalType().name()
        );
        entity.setGapPercent(signal.getGapPercent());
        entity.setMarketCap(signal.getMarketCap());
        entity.setTradeValue(signal.getTradeValue());
        entity.setTradeStrength(signal.getTradeStrength());
        entity.setHighPrice(signal.getHighPrice());
        entity.setPullbackPercent(signal.getPullbackPercent());
        entity.setBouncePercent(signal.getBouncePercent());
        entity.setCurrentPrice(signal.getCurrentPrice());
        entity.setExecuted(signal.isExecuted());
        entity.setCreatedAt(signal.getCreatedAt());
        return entity;
    }

    /**
     * 저장된 시각·수치를 그대로 복원한다 — 생성자로 복원하면 signalTime·createdAt 이 조회 시각, 수치가 전부 null
     * 이었다 (ADR stock/observability/0003).
     */
    private StockSignal toDomain(StockSignalJpaEntity entity) {
        return StockSignal.restore(
            entity.getId(),
            entity.getStockCode(),
            StockSignalType.valueOf(entity.getSignalType()),
            entity.getSignalTime(),
            entity.getGapPercent(),
            entity.getMarketCap(),
            entity.getTradeValue(),
            entity.getTradeStrength(),
            entity.getHighPrice(),
            entity.getPullbackPercent(),
            entity.getBouncePercent(),
            entity.getCurrentPrice(),
            Boolean.TRUE.equals(entity.getExecuted()),
            entity.getCreatedAt()
        );
    }
}
