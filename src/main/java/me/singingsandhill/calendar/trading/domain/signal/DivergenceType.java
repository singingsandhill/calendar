package me.singingsandhill.calendar.trading.domain.signal;

public enum DivergenceType {
    BULLISH,    // 강세 다이버전스 (가격 저점 하락, 지표 저점 상승)
    BEARISH,    // 약세 다이버전스 (가격 고점 상승, 지표 고점 하락)
    NONE
}
