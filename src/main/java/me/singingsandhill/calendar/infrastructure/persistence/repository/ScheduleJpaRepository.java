package me.singingsandhill.calendar.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.singingsandhill.calendar.infrastructure.persistence.entity.ScheduleJpaEntity;

public interface ScheduleJpaRepository extends JpaRepository<ScheduleJpaEntity, Long> {

    @Query("SELECT s FROM ScheduleJpaEntity s WHERE s.owner.ownerId = :ownerId AND s.year = :year AND s.month = :month")
    Optional<ScheduleJpaEntity> findByOwnerIdAndYearMonth(
            @Param("ownerId") String ownerId,
            @Param("year") int year,
            @Param("month") int month);

    @Query("SELECT s FROM ScheduleJpaEntity s WHERE s.owner.ownerId = :ownerId ORDER BY s.year DESC, s.month DESC")
    List<ScheduleJpaEntity> findAllByOwnerId(@Param("ownerId") String ownerId);

    @Query("SELECT COUNT(s) > 0 FROM ScheduleJpaEntity s WHERE s.owner.ownerId = :ownerId AND s.year = :year AND s.month = :month")
    boolean existsByOwnerIdAndYearMonth(
            @Param("ownerId") String ownerId,
            @Param("year") int year,
            @Param("month") int month);
}
