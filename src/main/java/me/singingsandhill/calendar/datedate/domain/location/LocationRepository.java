package me.singingsandhill.calendar.datedate.domain.location;

import java.util.List;
import java.util.Optional;

public interface LocationRepository {

    Optional<Location> findById(Long id);

    List<Location> findAllByScheduleId(Long scheduleId);

    Location save(Location location);

    void delete(Location location);

    boolean existsByScheduleIdAndName(Long scheduleId, String name);

    List<Location> findAllOrderByPopularity();

    long count();

    long countAllVotes();
}
