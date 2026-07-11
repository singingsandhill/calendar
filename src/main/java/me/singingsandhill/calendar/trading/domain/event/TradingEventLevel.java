package me.singingsandhill.calendar.trading.domain.event;

public enum TradingEventLevel {
    OK,
    NOTICE,
    WARNING,
    CRITICAL;

    public boolean atLeast(TradingEventLevel min) {
        return this.ordinal() >= min.ordinal();
    }
}
