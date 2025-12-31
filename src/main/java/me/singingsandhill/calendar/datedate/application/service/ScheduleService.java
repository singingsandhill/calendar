package me.singingsandhill.calendar.datedate.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.exception.DuplicateScheduleException;
import me.singingsandhill.calendar.datedate.application.exception.ScheduleNotFoundException;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final OwnerRepository ownerRepository;

    public ScheduleService(ScheduleRepository scheduleRepository, OwnerRepository ownerRepository) {
        this.scheduleRepository = scheduleRepository;
        this.ownerRepository = ownerRepository;
    }

    public Schedule getSchedule(Long scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
    }

    public Schedule getScheduleByOwnerAndYearMonth(String ownerId, int year, int month) {
        return scheduleRepository.findByOwnerIdAndYearMonth(ownerId, year, month)
                .orElseThrow(() -> new ScheduleNotFoundException(ownerId, year, month));
    }

    public Schedule findScheduleByOwnerAndYearMonth(String ownerId, int year, int month) {
        return scheduleRepository.findByOwnerIdAndYearMonth(ownerId, year, month).orElse(null);
    }

    public List<Schedule> getSchedulesByOwnerId(String ownerId) {
        return scheduleRepository.findAllByOwnerId(ownerId);
    }

    @Transactional
    public Schedule createSchedule(String ownerId, int year, int month, Integer weeks) {
        ensureOwnerExists(ownerId);

        if (scheduleRepository.existsByOwnerIdAndYearMonth(ownerId, year, month)) {
            throw new DuplicateScheduleException(ownerId, year, month);
        }

        Schedule schedule = new Schedule(ownerId, year, month, weeks);
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(String ownerId, int year, int month) {
        Schedule schedule = scheduleRepository.findByOwnerIdAndYearMonth(ownerId, year, month)
                .orElseThrow(() -> new ScheduleNotFoundException(ownerId, year, month));
        scheduleRepository.delete(schedule);
    }

    @Transactional
    public Schedule updateSchedule(String ownerId, int year, int month, Integer weeks) {
        Schedule schedule = scheduleRepository.findByOwnerIdAndYearMonth(ownerId, year, month)
                .orElseThrow(() -> new ScheduleNotFoundException(ownerId, year, month));

        if (weeks != null) {
            Schedule updatedSchedule = new Schedule(
                    schedule.getId(),
                    schedule.getOwnerId(),
                    schedule.getYear(),
                    schedule.getMonth(),
                    weeks,
                    schedule.getCreatedAt(),
                    schedule.getParticipants()
            );
            return scheduleRepository.save(updatedSchedule);
        }

        return schedule;
    }

    private void ensureOwnerExists(String ownerId) {
        if (!ownerRepository.existsById(ownerId)) {
            Owner newOwner = new Owner(ownerId);
            ownerRepository.save(newOwner);
        }
    }
}
