package me.singingsandhill.calendar.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import me.singingsandhill.calendar.domain.menu.Menu;

public record MenuResponse(
        Long id,
        String name,
        String url,
        List<String> voters,
        int voteCount,
        LocalDateTime createdAt
) {
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getUrl(),
                menu.getVoters(),
                menu.getVoteCount(),
                menu.getCreatedAt()
        );
    }
}
