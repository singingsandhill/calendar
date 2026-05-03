package me.singingsandhill.calendar.stock.infrastructure.persistence.repository;

import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.EntryAttemptJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EntryAttemptJpaRepository extends JpaRepository<EntryAttemptJpaEntity, Long> {

    List<EntryAttemptJpaEntity> findByTradingDate(LocalDate tradingDate);

    long countByTradingDateAndAccepted(LocalDate tradingDate, boolean accepted);
}
