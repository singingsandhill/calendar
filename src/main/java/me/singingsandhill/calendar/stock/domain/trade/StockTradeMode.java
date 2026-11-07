package me.singingsandhill.calendar.stock.domain.trade;

/**
 * 체결·포지션이 만들어질 때의 봇 운영 모드 (ADR stock/observability/0003).
 *
 * 값은 {@code StockProperties.Bot.Mode} 와 같은 이름이다. 컬럼이 생기기 전 행은 NULL — 백필 전 레거시로 보고
 * LIVE 집계에 넣는다(백필 SQL 실행 후에는 NULL 이 남지 않는다).
 */
public enum StockTradeMode {
    LIVE,
    PAPER,
    BACKTEST;

    /** 이 모드로 집계할 때 rowMode 행을 포함하는지 — NULL 은 LIVE 로 취급. */
    public boolean includes(StockTradeMode rowMode) {
        return this == LIVE ? (rowMode == null || rowMode == LIVE) : rowMode == this;
    }

    public static StockTradeMode fromName(String name) {
        return name != null ? valueOf(name) : null;
    }
}
