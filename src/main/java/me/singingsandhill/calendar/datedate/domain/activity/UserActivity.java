package me.singingsandhill.calendar.datedate.domain.activity;

import java.time.LocalDateTime;

/**
 * 로그인 사용자의 활동 이벤트 (append-only, ADR datedate/domain/0005).
 * 기존 익명 데이터 구조(voters 문자열 등)를 건드리지 않기 위한 별도 기록.
 * targetId: PARTICIPATION=participantId, *_VOTE=location/menu id, SCHEDULE_CREATED=scheduleId.
 */
public class UserActivity {

    private final Long id;
    private final Long userId;
    private final ActivityType type;
    private final Long scheduleId;
    private final Long targetId;
    private final String detail;
    private final LocalDateTime occurredAt;

    public UserActivity(Long id, Long userId, ActivityType type, Long scheduleId,
                        Long targetId, String detail, LocalDateTime occurredAt) {
        if (userId == null || type == null) {
            throw new IllegalArgumentException("userId and type are required");
        }
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.scheduleId = scheduleId;
        this.targetId = targetId;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public ActivityType getType() {
        return type;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}
