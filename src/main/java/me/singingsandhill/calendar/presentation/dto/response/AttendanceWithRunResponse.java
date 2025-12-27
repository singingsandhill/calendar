package me.singingsandhill.calendar.presentation.dto.response;

import me.singingsandhill.calendar.domain.runner.Attendance;
import me.singingsandhill.calendar.domain.runner.Run;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public record AttendanceWithRunResponse(
    Long attendanceId,
    Long runId,
    LocalDate date,
    String formattedDate,
    LocalTime time,
    String formattedTime,
    String location,
    String category,
    String categoryDisplayName,
    BigDecimal distance
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static AttendanceWithRunResponse from(Attendance attendance, Run run) {
        return new AttendanceWithRunResponse(
            attendance.getId(),
            run.getId(),
            run.getDate(),
            run.getDate().format(DATE_FORMATTER),
            run.getTime(),
            run.getTime().format(TIME_FORMATTER),
            run.getLocation(),
            run.getCategory().name(),
            run.getCategory().getDisplayName(),
            attendance.getDistance()
        );
    }
}
