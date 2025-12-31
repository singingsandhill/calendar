package me.singingsandhill.calendar.trading.domain.position;

public enum CloseReason {
    STOP_LOSS,      // 손절
    TAKE_PROFIT,    // 익절
    SIGNAL,         // 신호 기반 청산
    MANUAL,         // 수동 청산
    REBALANCE       // 리밸런싱
}
