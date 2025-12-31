package me.singingsandhill.calendar.runner.presentation.dto.response;

import me.singingsandhill.calendar.runner.domain.MemberAttendanceStatsDto;

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
