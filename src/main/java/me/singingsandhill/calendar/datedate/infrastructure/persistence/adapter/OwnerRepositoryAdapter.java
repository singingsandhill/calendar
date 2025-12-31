package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantColor;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.converter.SelectionConverter;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.OwnerJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.ParticipantJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.ScheduleJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.OwnerJpaRepository;

@Repository
public class OwnerRepositoryAdapter implements OwnerRepository {

    private final OwnerJpaRepository jpaRepository;

    public OwnerRepositoryAdapter(OwnerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Owner> findById(String ownerId) {
        return jpaRepository.findById(ownerId)
                .map(this::toDomain);
    }

    @Override
    public Owner save(Owner owner) {
        OwnerJpaEntity entity = toEntity(owner);
        OwnerJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public boolean existsById(String ownerId) {
        return jpaRepository.existsById(ownerId);
    }

    private Owner toDomain(OwnerJpaEntity entity) {
        return new Owner(
                entity.getOwnerId(),
                entity.getCreatedAt(),
                entity.getSchedules().stream()
                        .map(this::scheduleToDomain)
                        .collect(Collectors.toList())
        );
    }

    private Schedule scheduleToDomain(ScheduleJpaEntity entity) {
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

    private OwnerJpaEntity toEntity(Owner owner) {
        return new OwnerJpaEntity(owner.getOwnerId(), owner.getCreatedAt());
    }
}
