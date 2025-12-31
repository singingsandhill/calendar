package me.singingsandhill.calendar.datedate.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.LocationJpaEntity;

public interface LocationJpaRepository extends JpaRepository<LocationJpaEntity, Long> {

    @Query("SELECT l FROM LocationJpaEntity l LEFT JOIN FETCH l.votes WHERE l.schedule.id = :scheduleId ORDER BY l.id")
    List<LocationJpaEntity> findAllByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT COUNT(l) > 0 FROM LocationJpaEntity l WHERE l.schedule.id = :scheduleId AND LOWER(l.name) = LOWER(:name)")
    boolean existsByScheduleIdAndName(@Param("scheduleId") Long scheduleId, @Param("name") String name);

    @Query("SELECT l FROM LocationJpaEntity l LEFT JOIN FETCH l.votes ORDER BY SIZE(l.votes) DESC, l.createdAt DESC")
    List<LocationJpaEntity> findAllOrderByPopularity();
}
