package me.singingsandhill.calendar.datedate.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.exception.InvalidSelectionException;
import me.singingsandhill.calendar.datedate.application.exception.ParticipantNotFoundException;
import me.singingsandhill.calendar.datedate.application.exception.ScheduleNotFoundException;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

@Service
@Transactional(readOnly = true)
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final ScheduleRepository scheduleRepository;

    public ParticipantService(ParticipantRepository participantRepository,
                               ScheduleRepository scheduleRepository) {
        this.participantRepository = participantRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public Participant getParticipant(Long participantId) {
        return participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));
    }

    public List<Participant> getParticipantsByScheduleId(Long scheduleId) {
        return participantRepository.findAllByScheduleId(scheduleId);
    }

    @Transactional
    public Participant addParticipant(Long scheduleId, String name) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));

        Participant participant = new Participant(scheduleId, name, schedule.nextColorIndex());
        schedule.addParticipant(participant);
        return participantRepository.save(participant);
    }

    @Transactional
    public void deleteParticipant(Long participantId) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));
        participantRepository.delete(participant);
    }

    @Transactional
    public Participant updateSelections(Long participantId, List<Integer> selections) {
        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ParticipantNotFoundException(participantId));

        Schedule schedule = scheduleRepository.findById(participant.getScheduleId())
                .orElseThrow(() -> new ScheduleNotFoundException(participant.getScheduleId()));

        int totalDays = schedule.getTotalDays();
        List<Integer> safeSelections = selections != null ? selections : List.of();

        try {
            participant.updateSelections(safeSelections, totalDays);
        } catch (IllegalArgumentException e) {
            throw new InvalidSelectionException(extractInvalidDay(safeSelections, totalDays), totalDays);
        }
        return participantRepository.save(participant);
    }

    private int extractInvalidDay(List<Integer> selections, int totalDays) {
        return selections.stream()
                .filter(d -> d < 1 || d > totalDays)
                .findFirst()
                .orElse(-1);
    }
}
