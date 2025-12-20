package me.singingsandhill.calendar.presentation.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import me.singingsandhill.calendar.domain.location.Location;
import me.singingsandhill.calendar.domain.menu.Menu;
import me.singingsandhill.calendar.domain.schedule.Schedule;

public record ScheduleDetailResponse(
        Long id,
        String ownerId,
        int year,
        int month,
        int weeks,
        int daysInMonth,
        int firstDayOfWeek,
        List<ParticipantResponse> participants,
        List<LocationResponse> locations,
        List<MenuResponse> menus,
        LocalDateTime createdAt
) {
    public static ScheduleDetailResponse from(Schedule schedule) {
        return from(schedule, List.of(), List.of());
    }

    public static ScheduleDetailResponse from(Schedule schedule, List<Location> locations, List<Menu> menus) {
        return new ScheduleDetailResponse(
                schedule.getId(),
                schedule.getOwnerId(),
                schedule.getYear(),
                schedule.getMonth(),
                schedule.getWeeks(),
                schedule.getDaysInMonth(),
                schedule.getFirstDayOfWeek(),
                schedule.getParticipants().stream()
                        .map(ParticipantResponse::from)
                        .collect(Collectors.toList()),
                locations.stream()
                        .map(LocationResponse::from)
                        .collect(Collectors.toList()),
                menus.stream()
                        .map(MenuResponse::from)
                        .collect(Collectors.toList()),
                schedule.getCreatedAt()
        );
    }
}
