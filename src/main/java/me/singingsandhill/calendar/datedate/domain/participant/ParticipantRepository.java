package me.singingsandhill.calendar.datedate.domain.participant;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository {

    Optional<Participant> findById(Long id);

    List<Participant> findAllByScheduleId(Long scheduleId);

    Participant save(Participant participant);

    void delete(Participant participant);

    int countByScheduleId(Long scheduleId);

    boolean existsByScheduleIdAndName(Long scheduleId, String name);

    long count();

    /** 모임 데이터 리포트용 — 전체 참여자의 고른 날짜와 일정 정보 (ADR datedate/domain/0013). */
    List<ParticipantAvailability> findAllAvailability();
}
