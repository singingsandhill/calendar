package me.singingsandhill.calendar.trading.replay;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 운영 DB 의 청산된 포지션 — 리플레이가 고정하는 실제 진입(시각·가격·수량)과 실제 출구. 시각은 KST 로 변환돼 있다.
 * recordedEntryFee 는 봇이 판정에 쓴 진입 수수료(2026-09-27 이전은 0 으로 기록됐다), recordedPnl 은 DB 의 실현손익.
 */
record ReplayPosition(long id, LocalDateTime openedAt, LocalDateTime closedAt, BigDecimal entryPrice, BigDecimal volume,
                      BigDecimal recordedEntryFee, BigDecimal exitPrice, String closeReason, BigDecimal recordedPnl) {
}
