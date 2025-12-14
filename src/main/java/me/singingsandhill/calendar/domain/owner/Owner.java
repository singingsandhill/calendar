package me.singingsandhill.calendar.domain.owner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import me.singingsandhill.calendar.domain.schedule.Schedule;

public class Owner {

    private static final Pattern OWNER_ID_PATTERN = Pattern.compile("^[a-z0-9-]+$");
    private static final int MIN_ID_LENGTH = 2;
    private static final int MAX_ID_LENGTH = 20;

    private final String ownerId;
    private final LocalDateTime createdAt;
    private final List<Schedule> schedules;

    public Owner(String ownerId) {
        this(ownerId, LocalDateTime.now(), new ArrayList<>());
    }

    public Owner(String ownerId, LocalDateTime createdAt, List<Schedule> schedules) {
        validateOwnerId(ownerId);
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.schedules = new ArrayList<>(schedules);
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
}
