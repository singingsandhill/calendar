package me.singingsandhill.calendar.datedate.application.dto;

public record InsightsOverviewDto(
        long totalSchedules,
        long totalParticipants,
        long totalLocations,
        long totalMenus,
        long totalVotes,
        PopularItemDto topLocation,
        PopularItemDto topMenu
) {
}
