package me.singingsandhill.calendar.datedate.domain.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {

    Optional<Schedule> findById(Long id);

    Optional<Schedule> findByOwnerIdAndYearMonth(String ownerId, int year, int month);

    List<Schedule> findAllByOwnerId(String ownerId);

    Schedule save(Schedule schedule);

    void delete(Schedule schedule);

    boolean existsByOwnerIdAndYearMonth(String ownerId, int year, int month);

    long count();

    /** 모임 데이터 리포트용 — 전체 일정의 생성 시각. */
    List<LocalDateTime> findAllCreatedAt();
}
