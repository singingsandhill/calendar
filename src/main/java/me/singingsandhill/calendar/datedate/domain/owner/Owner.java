package me.singingsandhill.calendar.datedate.domain.owner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import me.singingsandhill.calendar.datedate.application.exception.OwnerAlreadyLinkedException;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;

public class Owner {

    private static final Pattern OWNER_ID_PATTERN = Pattern.compile("^[a-z0-9-]+$");
    private static final int MIN_ID_LENGTH = 2;
    private static final int MAX_ID_LENGTH = 20;

    private final String ownerId;
    private final LocalDateTime createdAt;
    private final List<Schedule> schedules;
    private Long userId;

    public Owner(String ownerId) {
        this(ownerId, LocalDateTime.now(), new ArrayList<>(), null);
    }

    public Owner(String ownerId, LocalDateTime createdAt, List<Schedule> schedules) {
        this(ownerId, createdAt, schedules, null);
    }

    /** first-claim 연결 (ADR datedate/domain/0005): 소유 증명 수단이 없어 선점 정책. */
    public Owner(String ownerId, LocalDateTime createdAt, List<Schedule> schedules, Long userId) {
        validateOwnerId(ownerId);
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.schedules = new ArrayList<>(schedules);
        this.userId = userId;
    }

    private void validateOwnerId(String ownerId) {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException("Owner ID cannot be blank");
        }
        if (ownerId.length() < MIN_ID_LENGTH || ownerId.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException(
                    "Owner ID must be between " + MIN_ID_LENGTH + " and " + MAX_ID_LENGTH + " characters");
        }
        if (!OWNER_ID_PATTERN.matcher(ownerId).matches()) {
            throw new IllegalArgumentException("Owner ID can only contain lowercase letters, numbers, and hyphens");
        }
    }

    public String getOwnerId() {
        return ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Schedule> getSchedules() {
        return Collections.unmodifiableList(schedules);
    }

    public void addSchedule(Schedule schedule) {
        schedules.add(schedule);
    }

    public int getScheduleCount() {
        return schedules.size();
    }

    public void linkUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }
        if (this.userId != null && !this.userId.equals(userId)) {
            throw new OwnerAlreadyLinkedException(ownerId);
        }
        this.userId = userId;
    }

    public boolean isLinkedTo(Long userId) {
        return this.userId != null && this.userId.equals(userId);
    }

    public Long getUserId() {
        return userId;
    }
}
