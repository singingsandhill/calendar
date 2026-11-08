package me.singingsandhill.calendar.trading.domain.regime;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 확정된 일봉 (KST 자정 기준). 레짐 판정은 종가만 쓰고, 리밸런싱 리플레이는 저가로 비상 손절을 판정한다.
 */
public record DailyBar(LocalDate date, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {
}
