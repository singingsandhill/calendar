package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivity;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivityRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.UserActivityJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.UserActivityJpaRepository;

@Repository
public class UserActivityRepositoryAdapter implements UserActivityRepository {

    private final UserActivityJpaRepository jpaRepository;

    public UserActivityRepositoryAdapter(UserActivityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public UserActivity save(UserActivity activity) {
        UserActivityJpaEntity saved = jpaRepository.save(new UserActivityJpaEntity(
                activity.getId(), activity.getUserId(), activity.getType(),
                activity.getScheduleId(), activity.getTargetId(), activity.getDetail(),
                activity.getOccurredAt()));
        return toDomain(saved);
    }

    @Override
    public boolean existsByUserIdAndTypeAndTargetId(Long userId, ActivityType type, Long targetId) {
        return jpaRepository.existsByUserIdAndTypeAndTargetId(userId, type, targetId);
    }

    @Override
    public List<UserActivity> findAllByUserIdAndOccurredAtBetween(Long userId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findAllByUserIdAndOccurredAtBetween(userId, from, to).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private UserActivity toDomain(UserActivityJpaEntity entity) {
        return new UserActivity(entity.getId(), entity.getUserId(), entity.getType(),
                entity.getScheduleId(), entity.getTargetId(), entity.getDetail(), entity.getOccurredAt());
    }
}
