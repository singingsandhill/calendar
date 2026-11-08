package me.singingsandhill.calendar.trading.domain.position;

public enum CloseReason {
    STOP_LOSS,       // 손절
    TAKE_PROFIT,     // 익절
    TRAILING_STOP,   // 트레일링 스탑
    SIGNAL,          // 신호 기반 청산
    MANUAL,          // 수동 청산
    REBALANCE,       // 리밸런싱
    TIME_EXIT,       // P2-8: 정체 포지션 최대 보유시간 초과 청산
    CATASTROPHIC_STOP // 리밸런싱 포지션 비상 손절 — 엔진이 켜져 있을 때 장중 SL/TP 대신 (ADR trading/risk/0007)
}
