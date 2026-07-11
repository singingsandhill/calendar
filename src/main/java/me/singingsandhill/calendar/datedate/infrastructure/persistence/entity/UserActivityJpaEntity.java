package me.singingsandhill.calendar.datedate.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;

@Entity
@Table(name = "user_activities",
        indexes = @Index(name = "idx_user_activities_user_occurred", columnList = "userId, occurredAt"))
public class UserActivityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityType type;

    private Long scheduleId;

    private Long targetId;

    @Column(length = 200)
    private String detail;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected UserActivityJpaEntity() {
    }

    public UserActivityJpaEntity(Long id, Long userId, ActivityType type, Long scheduleId,
                                 Long targetId, String detail, LocalDateTime occurredAt) {
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
