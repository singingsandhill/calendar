package me.singingsandhill.calendar.trading.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.trading.domain.signal.DivergenceType;
import me.singingsandhill.calendar.trading.domain.signal.Signal;
import me.singingsandhill.calendar.trading.domain.signal.SignalRepository;
import me.singingsandhill.calendar.trading.domain.signal.SignalType;
import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.SignalJpaEntity;
import me.singingsandhill.calendar.trading.infrastructure.persistence.repository.SignalJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class SignalRepositoryAdapter implements SignalRepository {

    private final SignalJpaRepository jpaRepository;

    public SignalRepositoryAdapter(SignalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Signal save(Signal signal) {
        SignalJpaEntity entity = toEntity(signal);
        SignalJpaEntity saved = jpaRepository.save(entity);
        signal.setId(saved.getId());
        return signal;
    }

    @Override
    public Optional<Signal> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Signal> findByMarketOrderBySignalTimeDesc(String market, int limit) {
        return jpaRepository.findByMarketOrderBySignalTimeDesc(market, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Signal> findLatestByMarket(String market) {
        return jpaRepository.findLatestByMarket(market).map(this::toDomain);
    }

    @Override
    public List<Signal> findByMarketAndSignalType(String market, SignalType signalType, int limit) {
        return jpaRepository.findByMarketAndSignalType(market, signalType.name(), PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Signal> findByMarketAndSignalTimeBetween(String market, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByMarketAndSignalTimeBetween(market, start, end)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByMarketAndExecuted(String market, boolean executed) {
        return jpaRepository.countByMarketAndExecuted(market, executed);
    }

    private SignalJpaEntity toEntity(Signal signal) {
        SignalJpaEntity entity = new SignalJpaEntity();
        if (signal.getId() != null) {
            entity.setId(signal.getId());
        }
        entity.setMarket(signal.getMarket());
        entity.setSignalTime(signal.getSignalTime());
        entity.setSignalType(signal.getSignalType().name());
        entity.setTotalScore(signal.getTotalScore());
        entity.setMaCrossScore(signal.getMaCrossScore());
        entity.setMaTrendScore(signal.getMaTrendScore());
        entity.setRsiDivergenceScore(signal.getRsiDivergenceScore());
        entity.setRsiLevelScore(signal.getRsiLevelScore());
        entity.setStochDivergenceScore(signal.getStochDivergenceScore());
        entity.setStochLevelScore(signal.getStochLevelScore());
        entity.setVolumeDivergenceScore(signal.getVolumeDivergenceScore());
        entity.setMa5(signal.getMa5());
        entity.setMa20(signal.getMa20());
        entity.setMa60(signal.getMa60());
        entity.setRsi(signal.getRsi());
        entity.setStochK(signal.getStochK());
        entity.setStochD(signal.getStochD());
        entity.setRsiDivergence(signal.getRsiDivergence() != null ? signal.getRsiDivergence().name() : null);
        entity.setStochDivergence(signal.getStochDivergence() != null ? signal.getStochDivergence().name() : null);
        entity.setVolumeDivergence(signal.getVolumeDivergence() != null ? signal.getVolumeDivergence().name() : null);
        entity.setCurrentPrice(signal.getCurrentPrice());
        entity.setExecuted(signal.isExecuted());
        entity.setCreatedAt(signal.getCreatedAt());
        return entity;
    }

    private Signal toDomain(SignalJpaEntity entity) {
        return new Signal(
                entity.getId(),
                entity.getMarket(),
                entity.getSignalTime(),
                SignalType.valueOf(entity.getSignalType()),
                entity.getTotalScore(),
                entity.getMaCrossScore(),
                entity.getMaTrendScore(),
                entity.getRsiDivergenceScore(),
                entity.getRsiLevelScore(),
                entity.getStochDivergenceScore(),
                entity.getStochLevelScore(),
                entity.getVolumeDivergenceScore(),
                entity.getMa5(),
                entity.getMa20(),
                entity.getMa60(),
                entity.getRsi(),
                entity.getStochK(),
                entity.getStochD(),
                entity.getRsiDivergence() != null ? DivergenceType.valueOf(entity.getRsiDivergence()) : null,
                entity.getStochDivergence() != null ? DivergenceType.valueOf(entity.getStochDivergence()) : null,
                entity.getVolumeDivergence() != null ? DivergenceType.valueOf(entity.getVolumeDivergence()) : null,
                entity.getCurrentPrice(),
                entity.isExecuted(),
                entity.getCreatedAt()
        );
    }
}
