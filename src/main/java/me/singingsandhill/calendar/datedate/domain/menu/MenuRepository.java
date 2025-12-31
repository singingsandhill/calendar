package me.singingsandhill.calendar.datedate.domain.menu;

import java.util.List;
import java.util.Optional;

public interface MenuRepository {

    Optional<Menu> findById(Long id);

    List<Menu> findAllByScheduleId(Long scheduleId);

    Menu save(Menu menu);

    void delete(Menu menu);

    boolean existsByScheduleIdAndName(Long scheduleId, String name);

    List<Menu> findAllOrderByPopularity();
}
