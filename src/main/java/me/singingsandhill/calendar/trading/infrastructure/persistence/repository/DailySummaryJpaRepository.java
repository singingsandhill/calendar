package me.singingsandhill.calendar.trading.infrastructure.persistence.repository;

import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.DailySummaryJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySummaryJpaRepository extends JpaRepository<DailySummaryJpaEntity, Long> {

    Optional<DailySummaryJpaEntity> findBySummaryDate(LocalDate summaryDate);

    @Query("SELECT d FROM DailySummaryJpaEntity d WHERE d.summaryDate BETWEEN :start AND :end ORDER BY d.summaryDate DESC")
    List<DailySummaryJpaEntity> findByDateBetweenOrderByDateDesc(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT d FROM DailySummaryJpaEntity d WHERE d.summaryDate > :date ORDER BY d.summaryDate DESC")
    List<DailySummaryJpaEntity> findBySummaryDateAfterOrderBySummaryDateDesc(@Param("date") LocalDate date);

    @Query("SELECT d FROM DailySummaryJpaEntity d ORDER BY d.summaryDate DESC")
    List<DailySummaryJpaEntity> findRecentDays(Pageable pageable);
}
