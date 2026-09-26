package me.singingsandhill.calendar.datedate.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.TimeSlotJpaEntity;

public interface TimeSlotJpaRepository extends JpaRepository<TimeSlotJpaEntity, Long> {

    @Query("SELECT DISTINCT t FROM TimeSlotJpaEntity t LEFT JOIN FETCH t.votes WHERE t.schedule.id = :scheduleId "
            + "ORDER BY t.dayIndex, t.startMinute, t.endMinute, t.id")
    List<TimeSlotJpaEntity> findAllByScheduleId(@Param("scheduleId") Long scheduleId);

    @Query("SELECT COUNT(t) > 0 FROM TimeSlotJpaEntity t WHERE t.schedule.id = :scheduleId "
            + "AND t.dayIndex = :dayIndex AND t.startMinute = :startMinute AND t.endMinute = :endMinute")
    boolean existsByScheduleIdAndRange(@Param("scheduleId") Long scheduleId,
                                       @Param("dayIndex") int dayIndex,
                                       @Param("startMinute") int startMinute,
                                       @Param("endMinute") int endMinute);
}
