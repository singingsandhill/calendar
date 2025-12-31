package me.singingsandhill.calendar.trading.infrastructure.persistence.repository;

import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.AccountSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AccountSnapshotJpaRepository extends JpaRepository<AccountSnapshotJpaEntity, Long> {

    @Query("SELECT a FROM AccountSnapshotJpaEntity a ORDER BY a.snapshotTime DESC LIMIT 1")
    Optional<AccountSnapshotJpaEntity> findLatest();

    @Query("SELECT a FROM AccountSnapshotJpaEntity a WHERE a.snapshotTime BETWEEN :start AND :end ORDER BY a.snapshotTime DESC")
    List<AccountSnapshotJpaEntity> findBySnapshotTimeBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM AccountSnapshotJpaEntity a WHERE a.snapshotTime BETWEEN :start AND :end ORDER BY a.snapshotTime ASC LIMIT 1")
    Optional<AccountSnapshotJpaEntity> findFirstBySnapshotTimeBetweenOrderBySnapshotTimeAsc(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM AccountSnapshotJpaEntity a WHERE a.snapshotTime BETWEEN :start AND :end ORDER BY a.snapshotTime DESC LIMIT 1")
    Optional<AccountSnapshotJpaEntity> findFirstBySnapshotTimeBetweenOrderBySnapshotTimeDesc(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM AccountSnapshotJpaEntity a WHERE a.snapshotTime > :timestamp ORDER BY a.snapshotTime DESC")
    List<AccountSnapshotJpaEntity> findBySnapshotTimeAfterOrderBySnapshotTimeDesc(
            @Param("timestamp") LocalDateTime timestamp);

    @Modifying
    @Query("DELETE FROM AccountSnapshotJpaEntity a WHERE a.createdAt < :dateTime")
    void deleteOlderThan(@Param("dateTime") LocalDateTime dateTime);
}
