package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlot;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlotRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.ScheduleJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.TimeSlotJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.TimeSlotVoteJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.ScheduleJpaRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.TimeSlotJpaRepository;

@Repository
public class TimeSlotRepositoryAdapter implements TimeSlotRepository {

    private final TimeSlotJpaRepository timeSlotJpaRepository;
    private final ScheduleJpaRepository scheduleJpaRepository;

    public TimeSlotRepositoryAdapter(TimeSlotJpaRepository timeSlotJpaRepository,
                                     ScheduleJpaRepository scheduleJpaRepository) {
        this.timeSlotJpaRepository = timeSlotJpaRepository;
        this.scheduleJpaRepository = scheduleJpaRepository;
    }

    @Override
    public Optional<TimeSlot> findById(Long id) {
        return timeSlotJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<TimeSlot> findAllByScheduleId(Long scheduleId) {
        return timeSlotJpaRepository.findAllByScheduleId(scheduleId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public TimeSlot save(TimeSlot timeSlot) {
        TimeSlotJpaEntity entity;

        if (timeSlot.getId() != null) {
            entity = timeSlotJpaRepository.findById(timeSlot.getId())
                    .orElseThrow(() -> new IllegalStateException("Time slot not found: " + timeSlot.getId()));

            // Update votes
            entity.getVotes().clear();
            for (String voter : timeSlot.getVoters()) {
                entity.addVote(new TimeSlotVoteJpaEntity(entity, voter));
            }
        } else {
            ScheduleJpaEntity schedule = scheduleJpaRepository.findById(timeSlot.getScheduleId())
                    .orElseThrow(() -> new IllegalStateException("Schedule not found: " + timeSlot.getScheduleId()));

            entity = new TimeSlotJpaEntity(schedule, timeSlot.getDayIndex(), timeSlot.getStartMinute(),
                    timeSlot.getEndMinute(), timeSlot.getCreatedAt());

            for (String voter : timeSlot.getVoters()) {
                entity.addVote(new TimeSlotVoteJpaEntity(entity, voter));
            }
        }

        TimeSlotJpaEntity saved = timeSlotJpaRepository.save(entity);
        TimeSlot result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public boolean existsByScheduleIdAndRange(Long scheduleId, int dayIndex, int startMinute, int endMinute) {
        return timeSlotJpaRepository.existsByScheduleIdAndRange(scheduleId, dayIndex, startMinute, endMinute);
    }

    private TimeSlot toDomain(TimeSlotJpaEntity entity) {
        List<String> voters = entity.getVotes().stream()
                .map(TimeSlotVoteJpaEntity::getVoterName)
                .collect(Collectors.toList());

        return new TimeSlot(
                entity.getId(),
                entity.getScheduleId(),
                entity.getDayIndex(),
                entity.getStartMinute(),
                entity.getEndMinute(),
                voters,
                entity.getCreatedAt()
        );
    }
}
