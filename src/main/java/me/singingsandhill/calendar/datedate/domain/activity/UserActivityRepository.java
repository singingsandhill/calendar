package me.singingsandhill.calendar.datedate.domain.activity;

import java.time.LocalDateTime;
import java.util.List;

public interface UserActivityRepository {

    UserActivity save(UserActivity activity);

    boolean existsByUserIdAndTypeAndTargetId(Long userId, ActivityType type, Long targetId);

    List<UserActivity> findAllByUserIdAndOccurredAtBetween(Long userId, LocalDateTime from, LocalDateTime to);
}
