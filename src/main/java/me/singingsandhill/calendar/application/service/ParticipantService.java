package me.singingsandhill.calendar.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.application.exception.DuplicateParticipantException;
import me.singingsandhill.calendar.application.exception.InvalidSelectionException;
import me.singingsandhill.calendar.application.exception.ParticipantLimitExceededException;
import me.singingsandhill.calendar.application.exception.ParticipantNotFoundException;
import me.singingsandhill.calendar.application.exception.ScheduleNotFoundException;
import me.singingsandhill.calendar.domain.participant.Participant;
import me.singingsandhill.calendar.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.domain.schedule.Schedule;
import me.singingsandhill.calendar.domain.schedule.ScheduleRepository;

@Service
@Transactional(readOnly = true)
public class ParticipantService {

    private static final int MAX_PARTICIPANTS = 8;

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

        int currentCount = participantRepository.countByScheduleId(scheduleId);
        if (currentCount >= MAX_PARTICIPANTS) {
            throw new ParticipantLimitExceededException();
        }

        if (participantRepository.existsByScheduleIdAndName(scheduleId, name)) {
            throw new DuplicateParticipantException(name);
        }

        Participant participant = new Participant(scheduleId, name, currentCount);
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

        if (selections != null) {
            for (Integer day : selections) {
                if (day < 1 || day > totalDays) {
                    throw new InvalidSelectionException(day, totalDays);
                }
            }
        }

        participant.updateSelections(selections != null ? selections : List.of(), totalDays);
        return participantRepository.save(participant);
    }
}
