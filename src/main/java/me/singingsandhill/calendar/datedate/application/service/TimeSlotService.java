package me.singingsandhill.calendar.datedate.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.exception.DuplicateTimeSlotException;
import me.singingsandhill.calendar.datedate.application.exception.InvalidTimeSlotException;
import me.singingsandhill.calendar.datedate.application.exception.ScheduleNotFoundException;
import me.singingsandhill.calendar.datedate.application.exception.TimeSlotNotFoundException;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlot;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlotRepository;

@Service
@Transactional(readOnly = true)
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final ScheduleRepository scheduleRepository;

    public TimeSlotService(TimeSlotRepository timeSlotRepository,
                           ScheduleRepository scheduleRepository) {
        this.timeSlotRepository = timeSlotRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public List<TimeSlot> getTimeSlotsByScheduleId(Long scheduleId) {
        return timeSlotRepository.findAllByScheduleId(scheduleId);
    }

    @Transactional
    public TimeSlot addTimeSlot(Long scheduleId, int dayIndex, int startMinute, int endMinute) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));

        if (!schedule.hasAvailabilityOn(dayIndex)) {
            throw new InvalidTimeSlotException(
                    "No participant has selected day " + dayIndex + " as available");
        }

        if (timeSlotRepository.existsByScheduleIdAndRange(scheduleId, dayIndex, startMinute, endMinute)) {
            throw new DuplicateTimeSlotException(dayIndex, startMinute, endMinute);
        }

        TimeSlot timeSlot = new TimeSlot(scheduleId, dayIndex, startMinute, endMinute);
        return timeSlotRepository.save(timeSlot);
    }

    @Transactional
    public TimeSlot vote(Long timeSlotId, String voterName) {
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new TimeSlotNotFoundException(timeSlotId));

        timeSlot.addVote(voterName);
        return timeSlotRepository.save(timeSlot);
    }

    @Transactional
    public TimeSlot unvote(Long timeSlotId, String voterName) {
        TimeSlot timeSlot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new TimeSlotNotFoundException(timeSlotId));

        timeSlot.removeVote(voterName);
        return timeSlotRepository.save(timeSlot);
    }
}
