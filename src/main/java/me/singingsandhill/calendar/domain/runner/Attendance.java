package me.singingsandhill.calendar.domain.runner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class Attendance {

    private static final BigDecimal MAX_DISTANCE = new BigDecimal("100.0");
    private static final BigDecimal MIN_DISTANCE = new BigDecimal("0.1");
    private static final int MAX_NAME_LENGTH = 50;

    private Long id;
    private final Long runId;
    private final String participantName;
    private final BigDecimal distance;
    private final LocalDateTime createdAt;

    public Attendance(Long runId, String participantName, BigDecimal distance) {
        this(null, runId, participantName, distance, LocalDateTime.now());
    }

    public Attendance(Long id, Long runId, String participantName,
                      BigDecimal distance, LocalDateTime createdAt) {
        validateRunId(runId);
        validateParticipantName(participantName);
        validateDistance(distance);
        this.id = id;
        this.runId = runId;
        this.participantName = participantName;
        this.distance = distance.setScale(1, RoundingMode.HALF_UP);
        this.createdAt = createdAt;
    }

    private void validateRunId(Long runId) {
        if (runId == null) {
            throw new IllegalArgumentException("Run ID cannot be null");
        }
    }

    private void validateParticipantName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Participant name cannot be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Participant name cannot exceed " + MAX_NAME_LENGTH + " characters");
        }
    }

    private void validateDistance(BigDecimal distance) {
        if (distance == null) {
            throw new IllegalArgumentException("Distance cannot be null");
        }
        if (distance.compareTo(MIN_DISTANCE) < 0 || distance.compareTo(MAX_DISTANCE) > 0) {
            throw new IllegalArgumentException("Distance must be between " + MIN_DISTANCE + " and " + MAX_DISTANCE + " km");
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunId() {
        return runId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
