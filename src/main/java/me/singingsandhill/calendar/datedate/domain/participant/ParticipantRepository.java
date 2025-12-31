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
}
