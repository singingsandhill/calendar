package me.singingsandhill.calendar.runner.presentation.dto.response;

import me.singingsandhill.calendar.runner.domain.Run;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public record RunResponse(
    Long id,
    LocalDate date,
    LocalTime time,
    String location,
    String category,
    String categoryDisplayName,
    LocalDateTime createdAt,
    String formattedDate,
    String formattedTime
) {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static RunResponse from(Run run) {
        return new RunResponse(
            run.getId(),
            run.getDate(),
            run.getTime(),
            run.getLocation(),
            run.getCategory().name(),
            run.getCategory().getDisplayName(),
            run.getCreatedAt(),
            run.getDate().format(DATE_FORMATTER),
            run.getTime().format(TIME_FORMATTER)
        );
    }
}
