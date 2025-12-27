package me.singingsandhill.calendar.domain.runner;

public record AttendanceRankingDto(
    String participantName,
    long attendanceCount
) {}
