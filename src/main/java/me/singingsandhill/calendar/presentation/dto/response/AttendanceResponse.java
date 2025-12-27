package me.singingsandhill.calendar.presentation.dto.response;

import me.singingsandhill.calendar.domain.runner.Attendance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AttendanceResponse(
    Long id,
    Long runId,
    String participantName,
    BigDecimal distance,
    LocalDateTime createdAt
) {
    public static AttendanceResponse from(Attendance attendance) {
        return new AttendanceResponse(
            attendance.getId(),
            attendance.getRunId(),
            attendance.getParticipantName(),
            attendance.getDistance(),
            attendance.getCreatedAt()
        );
    }
}
