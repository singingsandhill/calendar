package me.singingsandhill.calendar.trading.replay;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 집계 단위 한 건 — 실제 포지션(B) 또는 시뮬레이션 출구(A). pnl 은 모델 수수료 차감 원화, pnlPct 는 진입금액 대비 %. */
record ReplayTrade(LocalDateTime openedAt, LocalDateTime closedAt, String reason, BigDecimal pnl, double pnlPct) {

    ReplayTrade scaled(double multiplier) {
        return new ReplayTrade(openedAt, closedAt, reason, pnl.multiply(BigDecimal.valueOf(multiplier)), pnlPct * multiplier);
    }
}
