package me.singingsandhill.calendar.stock.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.domain.stock.StockState;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@Transactional(readOnly = true)
public class StockRepositoryAdapter implements StockRepository {

    private final StockJpaRepository jpaRepository;

    public StockRepositoryAdapter(StockJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Stock save(Stock stock) {
        StockJpaEntity entity;

        if (stock.getId() != null) {
            entity = jpaRepository.findById(stock.getId())
                .orElseThrow(() -> new IllegalStateException("Stock not found: " + stock.getId()));
            updateEntity(entity, stock);
        } else {
            entity = toEntity(stock);
        }

        StockJpaEntity saved = jpaRepository.save(entity);
        Stock result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    @Transactional
    public List<Stock> saveAll(List<Stock> stocks) {
        return stocks.stream()
            .map(this::save)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Stock> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Stock> findByStockCodeAndTradingDate(String stockCode, LocalDate tradingDate) {
        return jpaRepository.findByStockCodeAndTradingDate(stockCode, tradingDate).map(this::toDomain);
    }

    @Override
    public List<Stock> findByTradingDate(LocalDate tradingDate) {
        return jpaRepository.findByTradingDate(tradingDate).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Stock> findByTradingDateAndState(LocalDate tradingDate, StockState state) {
        return jpaRepository.findByTradingDateAndState(tradingDate, state.name()).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Stock> findByTradingDateAndStateIn(LocalDate tradingDate, List<StockState> states) {
        List<String> stateNames = states.stream().map(Enum::name).collect(Collectors.toList());
        return jpaRepository.findByTradingDateAndStateIn(tradingDate, stateNames).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Stock> findByTradingDateOrderByGapPercentDesc(LocalDate tradingDate) {
        return jpaRepository.findByTradingDateOrderByGapPercentDesc(tradingDate).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Stock> findActiveStocks(LocalDate tradingDate) {
        return jpaRepository.findActiveStocks(tradingDate).stream()
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
    public void deleteByTradingDateBefore(LocalDate date) {
        jpaRepository.deleteByTradingDateBefore(date);
    }

    @Override
    public int countByTradingDateAndState(LocalDate tradingDate, StockState state) {
        return jpaRepository.countByTradingDateAndState(tradingDate, state.name());
    }

    private StockJpaEntity toEntity(Stock stock) {
        StockJpaEntity entity = new StockJpaEntity(
            stock.getStockCode(),
            stock.getStockName(),
            stock.getTradingDate(),
            stock.getState().name()
        );
        updateEntity(entity, stock);
        return entity;
    }

    private void updateEntity(StockJpaEntity entity, Stock stock) {
        entity.setPrevClosePrice(stock.getPrevClosePrice());
        entity.setPrevVolume(stock.getPrevVolume());
        entity.setOpenPrice(stock.getOpenPrice());
        entity.setCurrentPrice(stock.getCurrentPrice());
        entity.setHighPrice(stock.getHighPrice());
        entity.setLowPrice(stock.getLowPrice());
        entity.setVolume(stock.getVolume());
        entity.setTradeValue(stock.getTradeValue());
        entity.setGapPercent(stock.getGapPercent());
        entity.setMarketCap(stock.getMarketCap());
        entity.setTradeStrength(stock.getTradeStrength());
        entity.setSpreadPercent(stock.getSpreadPercent());
        entity.setState(stock.getState().name());
        entity.setHighAfterOpen(stock.getHighAfterOpen());
        entity.setHighFormedAt(stock.getHighFormedAt());
        entity.setPullbackLow(stock.getPullbackLow());
        entity.setPullbackStartAt(stock.getPullbackStartAt());
        entity.setEntryPrice(stock.getEntryPrice());
    }

    private Stock toDomain(StockJpaEntity entity) {
        Stock stock = new Stock(entity.getStockCode(), entity.getStockName(), entity.getTradingDate());
        stock.setId(entity.getId());
        stock.setPrevClosePrice(entity.getPrevClosePrice());
        stock.setPrevVolume(entity.getPrevVolume());
        stock.setOpenPrice(entity.getOpenPrice());
        stock.setCurrentPrice(entity.getCurrentPrice());
        stock.setHighPrice(entity.getHighPrice());
        stock.setLowPrice(entity.getLowPrice());
        stock.setVolume(entity.getVolume());
        stock.setTradeValue(entity.getTradeValue());
        stock.setGapPercent(entity.getGapPercent());
        stock.setMarketCap(entity.getMarketCap());
        stock.setTradeStrength(entity.getTradeStrength());
        stock.setSpreadPercent(entity.getSpreadPercent());
        stock.updateState(StockState.valueOf(entity.getState()));
        return stock;
    }
}
