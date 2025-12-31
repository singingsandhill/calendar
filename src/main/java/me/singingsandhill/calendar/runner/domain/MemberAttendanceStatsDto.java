package me.singingsandhill.calendar.runner.domain;

public record MemberAttendanceStatsDto(
    String participantName,
    long regularCount,
    long lightningCount,
    long totalCount
) {}
