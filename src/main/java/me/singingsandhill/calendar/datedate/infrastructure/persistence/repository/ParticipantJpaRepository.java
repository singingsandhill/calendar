package me.singingsandhill.calendar.datedate.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.ParticipantJpaEntity;

public interface ParticipantJpaRepository extends JpaRepository<ParticipantJpaEntity, Long> {

    @Query("SELECT p FROM ParticipantJpaEntity p WHERE p.schedule.id = :scheduleId ORDER BY p.id")
    List<ParticipantJpaEntity> findAllByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT COUNT(p) FROM ParticipantJpaEntity p WHERE p.schedule.id = :scheduleId")
    int countByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT COUNT(p) > 0 FROM ParticipantJpaEntity p WHERE p.schedule.id = :scheduleId AND LOWER(p.name) = LOWER(:name)")
    boolean existsByScheduleIdAndName(@Param("scheduleId") Long scheduleId, @Param("name") String name);
}
