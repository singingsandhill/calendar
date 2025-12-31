package me.singingsandhill.calendar.trading.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.trading.domain.candle.Candle;
import me.singingsandhill.calendar.trading.domain.candle.CandleRepository;
import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.CandleJpaEntity;
import me.singingsandhill.calendar.trading.infrastructure.persistence.repository.CandleJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class CandleRepositoryAdapter implements CandleRepository {

    private final CandleJpaRepository jpaRepository;

    public CandleRepositoryAdapter(CandleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Candle save(Candle candle) {
        CandleJpaEntity entity = toEntity(candle);
        CandleJpaEntity saved = jpaRepository.save(entity);
        candle.setId(saved.getId());
        return candle;
    }

    @Override
    @Transactional
    public List<Candle> saveAll(List<Candle> candles) {
        List<CandleJpaEntity> entities = candles.stream().map(this::toEntity).toList();
        List<CandleJpaEntity> savedEntities = jpaRepository.saveAll(entities);
        for (int i = 0; i < candles.size(); i++) {
            candles.get(i).setId(savedEntities.get(i).getId());
        }
        return candles;
    }

    @Override
    public Optional<Candle> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Candle> findByMarketOrderByDateTimeDesc(String market, int limit) {
        return jpaRepository.findByMarketOrderByDateTimeDesc(market, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Candle> findLatestByMarket(String market) {
        return jpaRepository.findLatestByMarket(market).map(this::toDomain);
    }

    @Override
    public Optional<Candle> findByMarketAndDateTime(String market, LocalDateTime dateTime) {
        return jpaRepository.findByMarketAndCandleDateTime(market, dateTime).map(this::toDomain);
    }

    @Override
    public Optional<Candle> findByMarketAndCandleDateTime(String market, LocalDateTime candleDateTime) {
        return jpaRepository.findByMarketAndCandleDateTime(market, candleDateTime).map(this::toDomain);
    }

    @Override
    public List<Candle> findByMarketAndDateTimeRange(String market, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByMarketAndCandleDateTimeBetween(market, from, to)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteOlderThan(LocalDateTime dateTime) {
        jpaRepository.deleteOlderThan(dateTime);
    }

    @Override
    @Transactional
    public int deleteByDateTimeBefore(LocalDateTime dateTime) {
        return jpaRepository.deleteByCandleDateTimeBefore(dateTime);
    }

    @Override
    public long countByMarket(String market) {
        return jpaRepository.countByMarket(market);
    }

    private CandleJpaEntity toEntity(Candle candle) {
        CandleJpaEntity entity = new CandleJpaEntity(
                candle.getMarket(),
                candle.getCandleDateTime(),
                candle.getOpeningPrice(),
                candle.getHighPrice(),
                candle.getLowPrice(),
                candle.getTradePrice(),
                candle.getVolume(),
                candle.getAccTradePrice(),
                candle.getCreatedAt()
        );
        if (candle.getId() != null) {
            entity.setId(candle.getId());
        }
        return entity;
    }

    private Candle toDomain(CandleJpaEntity entity) {
        return new Candle(
                entity.getId(),
                entity.getMarket(),
                entity.getCandleDateTime(),
                entity.getOpeningPrice(),
                entity.getHighPrice(),
                entity.getLowPrice(),
                entity.getTradePrice(),
                entity.getVolume(),
                entity.getAccTradePrice(),
                entity.getCreatedAt()
        );
    }
}
