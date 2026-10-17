package me.singingsandhill.calendar.datedate.domain.timeslot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 시간 투표 후보 — 특정 날짜(dayIndex)의 시간대 [startMinute, endMinute).
 * dayIndex 는 Participant.selections 와 같은 의미의 달력 인덱스, 시간은 자정 기준 분(0~1440)이다.
 */
public class TimeSlot {

    public static final int SLOT_MINUTES = 30;
    public static final int MINUTES_PER_DAY = 24 * 60;

    private Long id;
    private final Long scheduleId;
    private final int dayIndex;
    private final int startMinute;
    private final int endMinute;
    private final List<String> voters;
    private final LocalDateTime createdAt;

    public TimeSlot(Long scheduleId, int dayIndex, int startMinute, int endMinute) {
        this(null, scheduleId, dayIndex, startMinute, endMinute, new ArrayList<>(), LocalDateTime.now());
    }

    public TimeSlot(Long id, Long scheduleId, int dayIndex, int startMinute, int endMinute,
                    List<String> voters, LocalDateTime createdAt) {
        validateRange(dayIndex, startMinute, endMinute);
        this.id = id;
        this.scheduleId = scheduleId;
        this.dayIndex = dayIndex;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.voters = new ArrayList<>(voters);
        this.createdAt = createdAt;
    }

    private void validateRange(int dayIndex, int startMinute, int endMinute) {
        if (dayIndex < 1) {
            throw new IllegalArgumentException("Day index must be at least 1");
        }
        if (startMinute % SLOT_MINUTES != 0 || endMinute % SLOT_MINUTES != 0) {
            throw new IllegalArgumentException("Time must be in " + SLOT_MINUTES + "-minute steps");
        }
        if (startMinute < 0 || endMinute > MINUTES_PER_DAY || startMinute >= endMinute) {
            throw new IllegalArgumentException("End time must be after start time within the same day");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public int getStartMinute() {
        return startMinute;
    }

    public int getEndMinute() {
        return endMinute;
    }

    public List<String> getVoters() {
        return Collections.unmodifiableList(voters);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public int getVoteCount() {
        return voters.size();
    }

    public boolean hasVoter(String voterName) {
        return voters.stream()
                .anyMatch(v -> v.equalsIgnoreCase(voterName));
    }

    public void addVote(String voterName) {
        if (voterName == null || voterName.isBlank()) {
            throw new IllegalArgumentException("Voter name cannot be blank");
        }
        if (hasVoter(voterName)) {
            throw new IllegalStateException("Already voted for this time slot");
        }
        voters.add(voterName);
    }

    public void removeVote(String voterName) {
        voters.removeIf(v -> v.equalsIgnoreCase(voterName));
    }
}
