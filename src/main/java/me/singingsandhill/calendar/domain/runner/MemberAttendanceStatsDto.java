package me.singingsandhill.calendar.domain.runner;

public record MemberAttendanceStatsDto(
    String participantName,
    long regularCount,
    long lightningCount,
    long totalCount
) {}
