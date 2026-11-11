package me.singingsandhill.calendar.trading.replay;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 운영 DB 의 매분 신호 (time 은 KST). bearishDivergence = RSI 또는 Stoch 약세 다이버전스 — 강한 매도 판별에 쓴다
 * ({@code TradingBotService.isStrongSellSignal}).
 */
record ReplaySignal(LocalDateTime time, String type, int score, boolean bearishDivergence, BigDecimal price) {
}
