package me.singingsandhill.calendar.stock.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.stock.domain.candle.CandleInterval;
import me.singingsandhill.calendar.stock.domain.candle.StockCandle;
import me.singingsandhill.calendar.stock.domain.candle.StockCandleRepository;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockCandleJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockCandleJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class StockCandleRepositoryAdapter implements StockCandleRepository {

    private final StockCandleJpaRepository jpaRepository;

    public StockCandleRepositoryAdapter(StockCandleJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public StockCandle save(StockCandle candle) {
        StockCandleJpaEntity entity = toEntity(candle);
        StockCandleJpaEntity saved = jpaRepository.save(entity);
        StockCandle result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    @Transactional
    public List<StockCandle> saveAll(List<StockCandle> candles) {
        List<StockCandleJpaEntity> entities = candles.stream()
            .map(this::toEntity)
            .collect(Collectors.toList());
        return jpaRepository.saveAll(entities).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<StockCandle> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<StockCandle> findByStockCodeAndCandleDateTimeAndInterval(
            String stockCode, LocalDateTime candleDateTime, CandleInterval interval) {
        return jpaRepository.findByStockCodeAndCandleDatetimeAndIntervalType(
            stockCode, candleDateTime, interval.name()).map(this::toDomain);
    }

    @Override
    public List<StockCandle> findByStockCodeAndIntervalOrderByDateTimeDesc(
            String stockCode, CandleInterval interval, int limit) {
        return jpaRepository.findByStockCodeAndIntervalTypeOrderByDatetimeDesc(
            stockCode, interval.name(), PageRequest.of(0, limit)).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<StockCandle> findLatestByStockCodeAndInterval(String stockCode, CandleInterval interval) {
        return jpaRepository.findLatestByStockCodeAndIntervalType(stockCode, interval.name())
            .map(this::toDomain);
    }

    @Override
    public List<StockCandle> findByStockCodeAndIntervalAndDateTimeRange(
            String stockCode, CandleInterval interval, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByStockCodeAndIntervalTypeAndDatetimeRange(
            stockCode, interval.name(), from, to).stream()
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
    public void deleteByDateTimeBefore(LocalDateTime dateTime) {
        jpaRepository.deleteByDatetimeBefore(dateTime);
    }

    private StockCandleJpaEntity toEntity(StockCandle candle) {
        return new StockCandleJpaEntity(
            candle.getStockCode(),
            candle.getCandleDateTime(),
            candle.getInterval().name(),
            candle.getOpenPrice(),
            candle.getHighPrice(),
            candle.getLowPrice(),
            candle.getClosePrice(),
            candle.getVolume(),
            candle.getTradeValue()
        );
    }

    private StockCandle toDomain(StockCandleJpaEntity entity) {
        StockCandle candle = StockCandle.of(
            entity.getStockCode(),
            entity.getCandleDatetime(),
            CandleInterval.valueOf(entity.getIntervalType()),
            entity.getOpenPrice(),
            entity.getHighPrice(),
            entity.getLowPrice(),
            entity.getClosePrice(),
            entity.getVolume(),
            entity.getTradeValue()
        );
        candle.setId(entity.getId());
        return candle;
    }
}
