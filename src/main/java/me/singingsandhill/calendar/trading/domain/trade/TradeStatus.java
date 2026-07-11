package me.singingsandhill.calendar.trading.domain.trade;

public enum TradeStatus {
    SUBMITTED,  // 주문 전송됨·결과 미확인 (§8-B 선영속화 — 틱 스윕이 정합화)
    WAIT,       // 체결 대기
    DONE,       // 체결 완료
    CANCEL,     // 주문 취소
    FAILED      // 주문 실패 (API 오류 등)
}
