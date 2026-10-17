package me.singingsandhill.calendar.datedate.domain.timeslot;

import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository {

    Optional<TimeSlot> findById(Long id);

    List<TimeSlot> findAllByScheduleId(Long scheduleId);

    TimeSlot save(TimeSlot timeSlot);

    boolean existsByScheduleIdAndRange(Long scheduleId, int dayIndex, int startMinute, int endMinute);
}
