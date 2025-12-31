package me.singingsandhill.calendar.datedate.domain.schedule;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {

    Optional<Schedule> findById(Long id);

    Optional<Schedule> findByOwnerIdAndYearMonth(String ownerId, int year, int month);

    List<Schedule> findAllByOwnerId(String ownerId);

    Schedule save(Schedule schedule);

    void delete(Schedule schedule);

    boolean existsByOwnerIdAndYearMonth(String ownerId, int year, int month);
}
