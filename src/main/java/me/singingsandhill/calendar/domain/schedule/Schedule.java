package me.singingsandhill.calendar.domain.schedule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.singingsandhill.calendar.domain.participant.Participant;

public class Schedule {

    private static final int MAX_PARTICIPANTS = 8;

    private Long id;
    private final String ownerId;
    private final YearMonth yearMonth;
    private final int weeks;
    private final LocalDateTime createdAt;
    private final List<Participant> participants;

    public Schedule(String ownerId, int year, int month) {
        this(null, ownerId, year, month, null, LocalDateTime.now(), new ArrayList<>());
    }

    public Schedule(String ownerId, int year, int month, Integer weeks) {
        this(null, ownerId, year, month, weeks, LocalDateTime.now(), new ArrayList<>());
    }

    public Schedule(Long id, String ownerId, int year, int month, Integer weeks,
                    LocalDateTime createdAt, List<Participant> participants) {
        this.id = id;
        this.ownerId = ownerId;
        this.yearMonth = YearMonth.of(year, month);
        this.weeks = weeks != null ? weeks : this.yearMonth.calculateWeeks();
        this.createdAt = createdAt;
        this.participants = new ArrayList<>(participants);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public int getYear() {
        return yearMonth.year();
    }

    public int getMonth() {
        return yearMonth.month();
    }

    public YearMonth getYearMonth() {
        return yearMonth;
    }

    public int getWeeks() {
        return weeks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(participants);
    }

    public int getParticipantCount() {
        return participants.size();
    }

    public int getDaysInMonth() {
        return yearMonth.getDaysInMonth();
    }

    public int getFirstDayOfWeek() {
        return yearMonth.getFirstDayOfWeek();
    }

    public boolean canAddParticipant() {
        return participants.size() < MAX_PARTICIPANTS;
    }

    public void addParticipant(Participant participant) {
        if (!canAddParticipant()) {
            throw new IllegalStateException("Maximum number of participants (" + MAX_PARTICIPANTS + ") reached");
        }
        participants.add(participant);
    }

    public boolean hasParticipantWithName(String name) {
        return participants.stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(name));
    }
}
