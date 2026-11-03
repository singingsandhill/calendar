package me.singingsandhill.calendar.stock.domain.stock;

/**
 * 진입(시장가 매수) 시도 결과 (ADR stock/infrastructure/0009).
 *
 * OPENED 외의 값은 {@link Stock#getLastEntryFailReason()} 으로 남는다. ORDER_UNCONFIRMED 는
 * 접수 여부가 불명이라 판정을 미확인 주문 스윕에 맡기므로 재시도 횟수에 넣지 않는다.
 */
public enum EntryOutcome {
    OPENED,
    /** 시세 조회 실패 */
    QUOTE_UNAVAILABLE,
    /** 거래정지·VI 등 거래 불가 상태 */
    NOT_TRADABLE,
    /** 계산된 수량 0 (예수금·매수가능수량 부족) */
    SIZE_ZERO,
    /** 주문 응답 유실·거부 — 스윕(reconcileUnconfirmedOrders)이 판정 */
    ORDER_UNCONFIRMED,
    /** 예기치 않은 예외 */
    ERROR;

    /** 최대 진입 시도 횟수 계산에 들어가는 실패인지. */
    public boolean countsAsAttempt() {
        return this != OPENED && this != ORDER_UNCONFIRMED;
    }
}
