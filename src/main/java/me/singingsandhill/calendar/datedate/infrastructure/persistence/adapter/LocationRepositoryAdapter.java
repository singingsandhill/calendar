package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.location.Location;
import me.singingsandhill.calendar.datedate.domain.location.LocationRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.LocationJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.LocationVoteJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.ScheduleJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.LocationJpaRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.ScheduleJpaRepository;

@Repository
public class LocationRepositoryAdapter implements LocationRepository {

    private final LocationJpaRepository locationJpaRepository;
    private final ScheduleJpaRepository scheduleJpaRepository;

    public LocationRepositoryAdapter(LocationJpaRepository locationJpaRepository,
                                      ScheduleJpaRepository scheduleJpaRepository) {
        this.locationJpaRepository = locationJpaRepository;
        this.scheduleJpaRepository = scheduleJpaRepository;
    }

    @Override
    public Optional<Location> findById(Long id) {
        return locationJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Location> findAllByScheduleId(Long scheduleId) {
        return locationJpaRepository.findAllByScheduleId(scheduleId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Location save(Location location) {
        LocationJpaEntity entity;

        if (location.getId() != null) {
            entity = locationJpaRepository.findById(location.getId())
                    .orElseThrow(() -> new IllegalStateException("Location not found: " + location.getId()));

            // Update votes
            entity.getVotes().clear();
            for (String voter : location.getVoters()) {
                entity.addVote(new LocationVoteJpaEntity(entity, voter));
            }
        } else {
            ScheduleJpaEntity schedule = scheduleJpaRepository.findById(location.getScheduleId())
                    .orElseThrow(() -> new IllegalStateException("Schedule not found: " + location.getScheduleId()));

            entity = new LocationJpaEntity(schedule, location.getName(), location.getCreatedAt());

            for (String voter : location.getVoters()) {
                entity.addVote(new LocationVoteJpaEntity(entity, voter));
            }
        }

        LocationJpaEntity saved = locationJpaRepository.save(entity);
        Location result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public void delete(Location location) {
        locationJpaRepository.deleteById(location.getId());
    }

    @Override
    public boolean existsByScheduleIdAndName(Long scheduleId, String name) {
        return locationJpaRepository.existsByScheduleIdAndName(scheduleId, name);
    }

    @Override
    public List<Location> findAllOrderByPopularity() {
        return locationJpaRepository.findAllOrderByPopularity().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public long count() {
        return locationJpaRepository.count();
    }

    @Override
    public long countAllVotes() {
        return locationJpaRepository.countAllVotes();
    }

    private Location toDomain(LocationJpaEntity entity) {
        List<String> voters = entity.getVotes().stream()
                .map(LocationVoteJpaEntity::getVoterName)
                .collect(Collectors.toList());

        return new Location(
                entity.getId(),
                entity.getScheduleId(),
                entity.getName(),
                voters,
                entity.getCreatedAt()
        );
    }
}
