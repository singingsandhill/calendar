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

    // 모임 데이터 리포트 — 일정과 함께 한 번에 읽는다(일정마다 지연 로딩하면 일정 수만큼 쿼리)
    @Query("SELECT p FROM ParticipantJpaEntity p JOIN FETCH p.schedule")
    List<ParticipantJpaEntity> findAllWithSchedule();
}
