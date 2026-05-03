package me.singingsandhill.calendar.stock.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.stock.domain.screening.EntryAttempt;
import me.singingsandhill.calendar.stock.domain.screening.EntryAttemptRepository;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.EntryAttemptJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.EntryAttemptJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class EntryAttemptRepositoryAdapter implements EntryAttemptRepository {

    private final EntryAttemptJpaRepository jpa;

    public EntryAttemptRepositoryAdapter(EntryAttemptJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public EntryAttempt save(EntryAttempt attempt) {
        EntryAttemptJpaEntity entity = EntryAttemptJpaEntity.fromDomain(attempt);
        EntryAttemptJpaEntity saved = jpa.save(entity);
        attempt.setId(saved.getId());
        return attempt;
    }

    @Override
    public List<EntryAttempt> findByTradingDate(LocalDate tradingDate) {
        return jpa.findByTradingDate(tradingDate).stream()
            .map(EntryAttemptJpaEntity::toDomain)
            .toList();
    }

    @Override
    public long countAcceptedByTradingDate(LocalDate tradingDate) {
        return jpa.countByTradingDateAndAccepted(tradingDate, true);
    }

    @Override
    public long countRejectedByTradingDate(LocalDate tradingDate) {
        return jpa.countByTradingDateAndAccepted(tradingDate, false);
    }
}
