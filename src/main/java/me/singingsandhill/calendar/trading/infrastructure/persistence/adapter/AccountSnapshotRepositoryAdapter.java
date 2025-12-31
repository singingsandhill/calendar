package me.singingsandhill.calendar.trading.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.trading.domain.account.AccountSnapshot;
import me.singingsandhill.calendar.trading.domain.account.AccountSnapshotRepository;
import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.AccountSnapshotJpaEntity;
import me.singingsandhill.calendar.trading.infrastructure.persistence.repository.AccountSnapshotJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class AccountSnapshotRepositoryAdapter implements AccountSnapshotRepository {

    private final AccountSnapshotJpaRepository jpaRepository;

    public AccountSnapshotRepositoryAdapter(AccountSnapshotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public AccountSnapshot save(AccountSnapshot snapshot) {
        AccountSnapshotJpaEntity entity = toEntity(snapshot);
        AccountSnapshotJpaEntity saved = jpaRepository.save(entity);
        snapshot.setId(saved.getId());
        return snapshot;
    }

    @Override
    public Optional<AccountSnapshot> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AccountSnapshot> findLatest() {
        return jpaRepository.findLatest().map(this::toDomain);
    }

    @Override
    public List<AccountSnapshot> findBySnapshotTimeBetween(LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findBySnapshotTimeBetween(start, end)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<AccountSnapshot> findFirstByMarketAndDateRange(String market, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findFirstBySnapshotTimeBetweenOrderBySnapshotTimeAsc(start, end)
                .map(this::toDomain);
    }

    @Override
    public Optional<AccountSnapshot> findLastByMarketAndDateRange(String market, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findFirstBySnapshotTimeBetweenOrderBySnapshotTimeDesc(start, end)
                .map(this::toDomain);
    }

    @Override
    public List<AccountSnapshot> findByMarketAndTimestampAfterOrderByTimestampDesc(String market, LocalDateTime timestamp) {
        return jpaRepository.findBySnapshotTimeAfterOrderBySnapshotTimeDesc(timestamp)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteOlderThan(LocalDateTime dateTime) {
        jpaRepository.deleteOlderThan(dateTime);
    }

    private AccountSnapshotJpaEntity toEntity(AccountSnapshot snapshot) {
        AccountSnapshotJpaEntity entity = new AccountSnapshotJpaEntity();
        if (snapshot.getId() != null) {
            entity.setId(snapshot.getId());
        }
        entity.setSnapshotTime(snapshot.getSnapshotTime());
        entity.setKrwBalance(snapshot.getKrwBalance());
        entity.setAdaBalance(snapshot.getAdaBalance());
        entity.setAdaAvgPrice(snapshot.getAdaAvgPrice());
        entity.setCurrentPrice(snapshot.getCurrentPrice());
        entity.setTotalValueKrw(snapshot.getTotalValueKrw());
        entity.setAdaRatio(snapshot.getAdaRatio());
        entity.setUnrealizedPnl(snapshot.getUnrealizedPnl());
        entity.setUnrealizedPnlPct(snapshot.getUnrealizedPnlPct());
        entity.setCreatedAt(snapshot.getCreatedAt());
        return entity;
    }

    private AccountSnapshot toDomain(AccountSnapshotJpaEntity entity) {
        return new AccountSnapshot(
                entity.getId(),
                entity.getSnapshotTime(),
                entity.getKrwBalance(),
                entity.getAdaBalance(),
                entity.getAdaAvgPrice(),
                entity.getCurrentPrice(),
                entity.getTotalValueKrw(),
                entity.getAdaRatio(),
                entity.getUnrealizedPnl(),
                entity.getUnrealizedPnlPct(),
                entity.getCreatedAt()
        );
    }
}
