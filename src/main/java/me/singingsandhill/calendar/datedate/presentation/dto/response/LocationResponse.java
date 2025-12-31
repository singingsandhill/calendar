package me.singingsandhill.calendar.datedate.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import me.singingsandhill.calendar.datedate.domain.location.Location;

public record LocationResponse(
        Long id,
        String name,
        List<String> voters,
        int voteCount,
        LocalDateTime createdAt
) {
    public static LocationResponse from(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getVoters(),
                location.getVoteCount(),
                location.getCreatedAt()
        );
    }
}
