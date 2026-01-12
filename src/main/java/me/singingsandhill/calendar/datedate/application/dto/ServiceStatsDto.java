package me.singingsandhill.calendar.datedate.application.dto;

public record ServiceStatsDto(
        long totalSchedules,
        long totalParticipants,
        long totalLocations,
        long totalMenus,
        long totalLocationVotes,
        long totalMenuVotes,
        double avgParticipantsPerSchedule
) {
}
