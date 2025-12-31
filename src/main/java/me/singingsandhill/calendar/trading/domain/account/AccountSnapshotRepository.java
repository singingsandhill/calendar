package me.singingsandhill.calendar.trading.domain.account;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountSnapshotRepository {

    AccountSnapshot save(AccountSnapshot snapshot);

    Optional<AccountSnapshot> findById(Long id);

    Optional<AccountSnapshot> findLatest();

    List<AccountSnapshot> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<AccountSnapshot> findFirstByMarketAndDateRange(String market, LocalDateTime start, LocalDateTime end);

    Optional<AccountSnapshot> findLastByMarketAndDateRange(String market, LocalDateTime start, LocalDateTime end);

    List<AccountSnapshot> findByMarketAndTimestampAfterOrderByTimestampDesc(String market, LocalDateTime timestamp);

    void deleteOlderThan(LocalDateTime dateTime);
}
