package me.singingsandhill.calendar.domain.runner;

import java.math.BigDecimal;

public record DistanceRankingDto(
    String participantName,
    BigDecimal totalDistance
) {}
