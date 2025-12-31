package me.singingsandhill.calendar.datedate.domain.participant;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Participant {

    private static final int MAX_NAME_LENGTH = 10;

    private Long id;
    private final Long scheduleId;
    private final String name;
    private final ParticipantColor color;
    private List<Integer> selections;
    private LocalDateTime updatedAt;

    public Participant(Long scheduleId, String name, int colorIndex) {
        this(null, scheduleId, name, ParticipantColor.ofIndex(colorIndex), new ArrayList<>(), LocalDateTime.now());
    }

    public Participant(Long scheduleId, String name, ParticipantColor color) {
        this(null, scheduleId, name, color, new ArrayList<>(), LocalDateTime.now());
    }

    public Participant(Long id, Long scheduleId, String name, ParticipantColor color,
                       List<Integer> selections, LocalDateTime updatedAt) {
        validateName(name);
        this.id = id;
        this.scheduleId = scheduleId;
        this.name = name;
        this.color = color;
        this.selections = new ArrayList<>(selections);
        this.updatedAt = updatedAt;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Participant name cannot be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Participant name cannot exceed " + MAX_NAME_LENGTH + " characters");
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

    public String getName() {
        return name;
    }

    public ParticipantColor getColor() {
        return color;
    }

    public String getColorHex() {
        return color.hexCode();
    }

    public List<Integer> getSelections() {
        return Collections.unmodifiableList(selections);
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void updateSelections(List<Integer> newSelections, int totalDays) {
        validateSelections(newSelections, totalDays);
        this.selections = new ArrayList<>(newSelections);
        this.updatedAt = LocalDateTime.now();
    }

    private void validateSelections(List<Integer> selections, int totalDays) {
        if (selections == null) {
            return;
        }
        for (Integer day : selections) {
            if (day < 1 || day > totalDays) {
                throw new IllegalArgumentException("Invalid day selection: " + day + ". Must be between 1 and " + totalDays);
            }
        }
    }
}
