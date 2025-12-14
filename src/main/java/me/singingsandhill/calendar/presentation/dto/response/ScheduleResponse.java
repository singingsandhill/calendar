package me.singingsandhill.calendar.presentation.dto.response;

import java.time.LocalDateTime;

import me.singingsandhill.calendar.domain.schedule.Schedule;

public record ScheduleResponse(
        Long id,
        String ownerId,
        int year,
        int month,
        int weeks,
        int participantCount,
        LocalDateTime createdAt
) {
    public String formattedYearMonth() {
        return String.format("%d-%02d", year, month);
    }

    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getOwnerId(),
                schedule.getYear(),
                schedule.getMonth(),
                schedule.getWeeks(),
                schedule.getParticipantCount(),
                schedule.getCreatedAt()
        );
    }
}
