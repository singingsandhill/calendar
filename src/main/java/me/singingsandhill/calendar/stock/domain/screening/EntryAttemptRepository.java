package me.singingsandhill.calendar.stock.domain.screening;

import java.time.LocalDate;
import java.util.List;

/**
 * 진입 시도 도메인 포트. JPA 어댑터는 infrastructure/persistence 에서 구현.
 */
public interface EntryAttemptRepository {

    EntryAttempt save(EntryAttempt attempt);

    List<EntryAttempt> findByTradingDate(LocalDate tradingDate);

    long countAcceptedByTradingDate(LocalDate tradingDate);

    long countRejectedByTradingDate(LocalDate tradingDate);
}
