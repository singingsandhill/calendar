package me.singingsandhill.calendar.datedate.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.UserActivityJpaEntity;

public interface UserActivityJpaRepository extends JpaRepository<UserActivityJpaEntity, Long> {

    boolean existsByUserIdAndTypeAndTargetId(Long userId, ActivityType type, Long targetId);

    List<UserActivityJpaEntity> findAllByUserIdAndOccurredAtBetween(
            Long userId, LocalDateTime from, LocalDateTime to);
}
