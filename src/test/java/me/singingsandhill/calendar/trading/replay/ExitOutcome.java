package me.singingsandhill.calendar.trading.replay;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 시뮬레이션한 출구. pnl 은 모델 수수료(양쪽) 차감 원화, pnlPct 는 진입금액 대비 %. */
record ExitOutcome(String reason, LocalDateTime exitTime, BigDecimal exitPrice, BigDecimal pnl, double pnlPct) {
}
