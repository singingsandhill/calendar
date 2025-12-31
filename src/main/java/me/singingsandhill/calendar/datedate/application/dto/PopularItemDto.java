package me.singingsandhill.calendar.datedate.application.dto;

import java.time.LocalDateTime;

public record PopularItemDto(
        String name,
        String url,
        int totalVotes,
        LocalDateTime latestCreatedAt
) {
}
