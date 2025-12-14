package me.singingsandhill.calendar.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.domain.participant.Participant;
import me.singingsandhill.calendar.domain.participant.ParticipantColor;
import me.singingsandhill.calendar.domain.schedule.Schedule;
import me.singingsandhill.calendar.domain.schedule.ScheduleRepository;
import me.singingsandhill.calendar.infrastructure.persistence.converter.SelectionConverter;
import me.singingsandhill.calendar.infrastructure.persistence.entity.OwnerJpaEntity;
import me.singingsandhill.calendar.infrastructure.persistence.entity.ParticipantJpaEntity;
import me.singingsandhill.calendar.infrastructure.persistence.entity.ScheduleJpaEntity;
import me.singingsandhill.calendar.infrastructure.persistence.repository.OwnerJpaRepository;
import me.singingsandhill.calendar.infrastructure.persistence.repository.ScheduleJpaRepository;

@Repository
public class ScheduleRepositoryAdapter implements ScheduleRepository {

    private final ScheduleJpaRepository scheduleJpaRepository;
    private final OwnerJpaRepository ownerJpaRepository;

    public ScheduleRepositoryAdapter(ScheduleJpaRepository scheduleJpaRepository,
                                      OwnerJpaRepository ownerJpaRepository) {
        this.scheduleJpaRepository = scheduleJpaRepository;
        this.ownerJpaRepository = ownerJpaRepository;
    }

    @Override
    public Optional<Schedule> findById(Long id) {
        return scheduleJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public Optional<Schedule> findByOwnerIdAndYearMonth(String ownerId, int year, int month) {
        return scheduleJpaRepository.findByOwnerIdAndYearMonth(ownerId, year, month)
                .map(this::toDomain);
    }

    @Override
    public List<Schedule> findAllByOwnerId(String ownerId) {
        return scheduleJpaRepository.findAllByOwnerId(ownerId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Schedule save(Schedule schedule) {
        OwnerJpaEntity owner = ownerJpaRepository.findById(schedule.getOwnerId())
                .orElseThrow(() -> new IllegalStateException("Owner not found: " + schedule.getOwnerId()));

        ScheduleJpaEntity entity;
        if (schedule.getId() != null) {
            entity = scheduleJpaRepository.findById(schedule.getId())
                    .orElseThrow(() -> new IllegalStateException("Schedule not found: " + schedule.getId()));
            entity.setWeeks(schedule.getWeeks());
        } else {
            entity = new ScheduleJpaEntity(
                    owner,
                    schedule.getYear(),
                    schedule.getMonth(),
                    schedule.getWeeks(),
                    schedule.getCreatedAt()
            );
        }

        ScheduleJpaEntity saved = scheduleJpaRepository.save(entity);
        Schedule result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public void delete(Schedule schedule) {
        scheduleJpaRepository.deleteById(schedule.getId());
    }

    @Override
    public boolean existsByOwnerIdAndYearMonth(String ownerId, int year, int month) {
        return scheduleJpaRepository.existsByOwnerIdAndYearMonth(ownerId, year, month);
    }

    private Schedule toDomain(ScheduleJpaEntity entity) {
        return new Schedule(
                entity.getId(),
                entity.getOwnerId(),
                entity.getYear(),
                entity.getMonth(),
                entity.getWeeks(),
                entity.getCreatedAt(),
                entity.getParticipants().stream()
                        .map(this::participantToDomain)
                        .collect(Collectors.toList())
        );
    }

    private Participant participantToDomain(ParticipantJpaEntity entity) {
        return new Participant(
                entity.getId(),
                entity.getScheduleId(),
                entity.getName(),
                new ParticipantColor(entity.getColor()),
                SelectionConverter.fromJson(entity.getSelections()),
                entity.getUpdatedAt()
        );
    }
}
