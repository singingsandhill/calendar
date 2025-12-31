package me.singingsandhill.calendar.trading.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.trading.domain.account.DailySummary;
import me.singingsandhill.calendar.trading.domain.account.DailySummaryRepository;
import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.DailySummaryJpaEntity;
import me.singingsandhill.calendar.trading.infrastructure.persistence.repository.DailySummaryJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class DailySummaryRepositoryAdapter implements DailySummaryRepository {

    private final DailySummaryJpaRepository jpaRepository;

    public DailySummaryRepositoryAdapter(DailySummaryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public DailySummary save(DailySummary summary) {
        DailySummaryJpaEntity entity = toEntity(summary);
        DailySummaryJpaEntity saved = jpaRepository.save(entity);
        summary.setId(saved.getId());
        return summary;
    }

    @Override
    public Optional<DailySummary> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<DailySummary> findByDate(LocalDate date) {
        return jpaRepository.findBySummaryDate(date).map(this::toDomain);
    }

    @Override
    public Optional<DailySummary> findByMarketAndDate(String market, LocalDate date) {
        return jpaRepository.findBySummaryDate(date).map(this::toDomain);
    }

    @Override
    public List<DailySummary> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end) {
        return jpaRepository.findByDateBetweenOrderByDateDesc(start, end)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DailySummary> findByMarketAndDateAfterOrderByDateDesc(String market, LocalDate date) {
        return jpaRepository.findBySummaryDateAfterOrderBySummaryDateDesc(date)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DailySummary> findRecentDays(int days) {
        return jpaRepository.findRecentDays(PageRequest.of(0, days))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private DailySummaryJpaEntity toEntity(DailySummary summary) {
        DailySummaryJpaEntity entity = new DailySummaryJpaEntity();
        if (summary.getId() != null) {
            entity.setId(summary.getId());
        }
        entity.setSummaryDate(summary.getSummaryDate());
        entity.setTradeCount(summary.getTradeCount());
        entity.setBuyCount(summary.getBuyCount());
        entity.setSellCount(summary.getSellCount());
        entity.setRealizedPnl(summary.getRealizedPnl());
        entity.setTotalVolume(summary.getTotalVolume());
        entity.setTotalAmount(summary.getTotalAmount());
        entity.setWinCount(summary.getWinCount());
        entity.setLoseCount(summary.getLoseCount());
        entity.setWinRate(summary.getWinRate());
        entity.setStartBalance(summary.getStartBalance());
        entity.setEndBalance(summary.getEndBalance());
        entity.setCreatedAt(summary.getCreatedAt());
        return entity;
    }

    private DailySummary toDomain(DailySummaryJpaEntity entity) {
        return new DailySummary(
                entity.getId(),
                entity.getSummaryDate(),
                entity.getTradeCount(),
                entity.getBuyCount(),
                entity.getSellCount(),
                entity.getRealizedPnl(),
                entity.getTotalVolume(),
                entity.getTotalAmount(),
                entity.getWinCount(),
                entity.getLoseCount(),
                entity.getWinRate(),
                entity.getStartBalance(),
                entity.getEndBalance(),
                entity.getCreatedAt()
        );
    }
}
