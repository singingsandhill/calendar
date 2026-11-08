package me.singingsandhill.calendar.trading.domain.regime;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 그날 종가까지 반영한 레짐 판정. movingAverage 는 데이터가 모자라면 null (그때 regime 은 UNKNOWN).
 */
public record RegimePoint(LocalDate date, MarketRegime regime, BigDecimal close, BigDecimal movingAverage) {
}
