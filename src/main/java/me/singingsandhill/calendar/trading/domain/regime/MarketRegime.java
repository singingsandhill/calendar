package me.singingsandhill.calendar.trading.domain.regime;

/**
 * 일봉 레짐 (ADR trading/risk/0007). UNKNOWN = 이동평균을 낼 만큼 데이터가 없거나 조회에 실패한 상태.
 */
public enum MarketRegime {
    BULL,
    BEAR,
    UNKNOWN
}
