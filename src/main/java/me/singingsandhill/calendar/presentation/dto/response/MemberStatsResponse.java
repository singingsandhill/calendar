package me.singingsandhill.calendar.presentation.dto.response;

import me.singingsandhill.calendar.domain.runner.MemberAttendanceStatsDto;

public record MemberStatsResponse(
    String name,
    long regularCount,
    long lightningCount,
    long totalCount
) {
    public static MemberStatsResponse from(MemberAttendanceStatsDto dto) {
        return new MemberStatsResponse(
            dto.participantName(),
            dto.regularCount(),
            dto.lightningCount(),
            dto.totalCount()
        );
    }
}
