package me.singingsandhill.calendar.trading.domain.account;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailySummaryRepository {

    DailySummary save(DailySummary summary);

    Optional<DailySummary> findById(Long id);

    Optional<DailySummary> findByDate(LocalDate date);

    Optional<DailySummary> findByMarketAndDate(String market, LocalDate date);

    List<DailySummary> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);

    List<DailySummary> findByMarketAndDateAfterOrderByDateDesc(String market, LocalDate date);

    List<DailySummary> findRecentDays(int days);
}
