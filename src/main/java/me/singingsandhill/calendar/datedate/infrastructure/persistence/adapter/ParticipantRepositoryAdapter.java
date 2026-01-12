package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantColor;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.converter.SelectionConverter;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.ParticipantJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.ScheduleJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.ParticipantJpaRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.ScheduleJpaRepository;

@Repository
public class ParticipantRepositoryAdapter implements ParticipantRepository {

    private final ParticipantJpaRepository participantJpaRepository;
    private final ScheduleJpaRepository scheduleJpaRepository;

    public ParticipantRepositoryAdapter(ParticipantJpaRepository participantJpaRepository,
                                         ScheduleJpaRepository scheduleJpaRepository) {
        this.participantJpaRepository = participantJpaRepository;
        this.scheduleJpaRepository = scheduleJpaRepository;
    }

    @Override
    public Optional<Participant> findById(Long id) {
        return participantJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Participant> findAllByScheduleId(Long scheduleId) {
        return participantJpaRepository.findAllByScheduleId(scheduleId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Participant save(Participant participant) {
        ParticipantJpaEntity entity;

        if (participant.getId() != null) {
            entity = participantJpaRepository.findById(participant.getId())
                    .orElseThrow(() -> new IllegalStateException("Participant not found: " + participant.getId()));
            entity.setSelections(SelectionConverter.toJson(participant.getSelections()));
            entity.setUpdatedAt(participant.getUpdatedAt());
        } else {
            ScheduleJpaEntity schedule = scheduleJpaRepository.findById(participant.getScheduleId())
                    .orElseThrow(() -> new IllegalStateException("Schedule not found: " + participant.getScheduleId()));

            entity = new ParticipantJpaEntity(
                    schedule,
                    participant.getName(),
                    participant.getColorHex(),
                    SelectionConverter.toJson(participant.getSelections()),
                    participant.getUpdatedAt()
            );
        }

        ParticipantJpaEntity saved = participantJpaRepository.save(entity);
        Participant result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public void delete(Participant participant) {
        participantJpaRepository.deleteById(participant.getId());
    }

    @Override
    public int countByScheduleId(Long scheduleId) {
        return participantJpaRepository.countByScheduleId(scheduleId);
    }

    @Override
    public boolean existsByScheduleIdAndName(Long scheduleId, String name) {
        return participantJpaRepository.existsByScheduleIdAndName(scheduleId, name);
    }

    @Override
    public long count() {
        return participantJpaRepository.count();
    }

    private Participant toDomain(ParticipantJpaEntity entity) {
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
