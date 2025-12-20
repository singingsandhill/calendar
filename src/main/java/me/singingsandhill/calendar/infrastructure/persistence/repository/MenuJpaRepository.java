package me.singingsandhill.calendar.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.singingsandhill.calendar.infrastructure.persistence.entity.MenuJpaEntity;

public interface MenuJpaRepository extends JpaRepository<MenuJpaEntity, Long> {

    @Query("SELECT m FROM MenuJpaEntity m LEFT JOIN FETCH m.votes WHERE m.schedule.id = :scheduleId ORDER BY m.id")
    List<MenuJpaEntity> findAllByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT COUNT(m) > 0 FROM MenuJpaEntity m WHERE m.schedule.id = :scheduleId AND LOWER(m.name) = LOWER(:name)")
    boolean existsByScheduleIdAndName(@Param("scheduleId") Long scheduleId, @Param("name") String name);
}
