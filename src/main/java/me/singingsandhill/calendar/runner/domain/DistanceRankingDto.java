package me.singingsandhill.calendar.runner.domain;

import java.math.BigDecimal;

public record DistanceRankingDto(
    String participantName,
    BigDecimal totalDistance
) {}
