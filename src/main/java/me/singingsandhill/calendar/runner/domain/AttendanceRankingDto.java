package me.singingsandhill.calendar.runner.domain;

public record AttendanceRankingDto(
    String participantName,
    long attendanceCount
) {}
